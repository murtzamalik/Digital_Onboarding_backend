package com.bank.cebos.controller.portal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.dto.portal.PortalNotificationItemResponse;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.portal.PortalNotificationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PortalNotificationControllerTest {

  private MockMvc mockMvc;
  private PortalNotificationService portalNotificationService;

  @BeforeAll
  void initOnce() {
    portalNotificationService = org.mockito.Mockito.mock(PortalNotificationService.class);
    CebosUserDetails portalUser =
        new CebosUserDetails(
            PrincipalKind.PORTAL,
            1L,
            42L,
            List.of(new SimpleGrantedAuthority(PortalRole.VIEWER.authority())));
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
    PortalNotificationController controller = new PortalNotificationController(portalNotificationService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(
                new PageableHandlerMethodArgumentResolver(), principalResolver)
            .build();
  }

  @Test
  void listNotificationsDelegatesToService() throws Exception {
    Instant t = Instant.parse("2026-04-01T12:00:00Z");
    when(portalNotificationService.listForClient(eq(42L), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new PortalNotificationItemResponse(
                        1L, "portal.password_reset", "SENT", "portal@e2e.local", t, t, null)),
                PageRequest.of(0, 20),
                1));

    mockMvc
        .perform(get("/api/v1/portal/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].templateKey").value("portal.password_reset"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(portalNotificationService).listForClient(eq(42L), any());
  }
}
