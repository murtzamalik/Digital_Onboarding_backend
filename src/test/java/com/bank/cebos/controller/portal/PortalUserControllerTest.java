package com.bank.cebos.controller.portal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.portal.PortalCorporateUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PortalUserControllerTest {

  private MockMvc mockMvc;
  private PortalCorporateUserService portalCorporateUserService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  void initOnce() {
    portalCorporateUserService = org.mockito.Mockito.mock(PortalCorporateUserService.class);
    CebosUserDetails portalUser =
        new CebosUserDetails(
            PrincipalKind.PORTAL,
            1L,
            42L,
            List.of(new SimpleGrantedAuthority(PortalRole.ADMIN.authority())));
    HandlerMethodArgumentResolver principalResolver =
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return CebosUserDetails.class.isAssignableFrom(parameter.getParameterType());
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return portalUser;
          }
        };
    PortalUserController controller = new PortalUserController(portalCorporateUserService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(
                new PageableHandlerMethodArgumentResolver(), principalResolver)
            .setControllerAdvice(new com.bank.cebos.exception.GlobalExceptionHandler())
            .build();
  }

  @Test
  void listUsersDelegatesToService() throws Exception {
    when(portalCorporateUserService.listForClient(eq(42L), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(new PortalCorporateUserResponse(9L, "a@b.c", "A User", "ADMIN", "ACTIVE")),
                PageRequest.of(0, 20),
                1));

    mockMvc
        .perform(get("/api/v1/portal/users").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("a@b.c"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(portalCorporateUserService).listForClient(eq(42L), any());
  }

  @Test
  void createUserReturnsBody() throws Exception {
    when(portalCorporateUserService.createForClient(eq(42L), any()))
        .thenReturn(new PortalCorporateUserResponse(11L, "new@b.c", "New", "VIEWER", "ACTIVE"));

    mockMvc
        .perform(
            post("/api/v1/portal/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new PortalCreateCorporateUserRequest(
                            "new@b.c", "longpassword", "New", "VIEWER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(11))
        .andExpect(jsonPath("$.role").value("VIEWER"));
  }
}
