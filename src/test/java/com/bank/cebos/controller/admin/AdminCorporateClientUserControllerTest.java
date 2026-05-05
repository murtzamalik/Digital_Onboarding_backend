package com.bank.cebos.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
import com.bank.cebos.service.admin.AdminCorporateClientUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminCorporateClientUserController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class AdminCorporateClientUserControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockBean private AdminCorporateClientUserService adminCorporateClientUserService;

  @MockBean private EmployeeOnboardingRepository employeeOnboardingRepository;

  @MockBean private UploadBatchRepository uploadBatchRepository;

  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean private RedisMinuteWindowLimiter redisMinuteWindowLimiter;

  @BeforeEach
  void jwtFilterDelegates() throws Exception {
    doAnswer(
            inv -> {
              FilterChain chain = inv.getArgument(2);
              chain.doFilter(
                  inv.getArgument(0, ServletRequest.class),
                  inv.getArgument(1, ServletResponse.class));
              return null;
            })
        .when(jwtAuthenticationFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  void listUsersReturns200WithPagedContent() throws Exception {
    PortalCorporateUserResponse row =
        new PortalCorporateUserResponse(10L, "a@corp.com", "Alice", "ADMIN", "ACTIVE");
    when(adminCorporateClientUserService.listUsers(eq(5L), any()))
        .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/v1/admin/clients/5/users").with(authentication(bankAdminAuth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].email").value("a@corp.com"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void createUserDelegatesToService() throws Exception {
    PortalCreateCorporateUserRequest req =
        new PortalCreateCorporateUserRequest(
            "new@corp.com", "Password12!", "New User", "VIEWER");
    PortalCorporateUserResponse body =
        new PortalCorporateUserResponse(99L, "new@corp.com", "New User", "VIEWER", "ACTIVE");
    when(adminCorporateClientUserService.createUser(eq(5L), any(PortalCreateCorporateUserRequest.class)))
        .thenReturn(body);

    mockMvc
        .perform(
            post("/api/v1/admin/clients/5/users")
                .with(authentication(bankAdminAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.role").value("VIEWER"));

    verify(adminCorporateClientUserService).createUser(eq(5L), any(PortalCreateCorporateUserRequest.class));
  }

  @Test
  void listUsersReturns404WhenServiceThrowsNotFound() throws Exception {
    when(adminCorporateClientUserService.listUsers(eq(5L), any()))
        .thenThrow(
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Client not found"));

    mockMvc
        .perform(get("/api/v1/admin/clients/5/users").with(authentication(bankAdminAuth())))
        .andExpect(status().isNotFound());
  }

  private static UsernamePasswordAuthenticationToken bankAdminAuth() {
    CebosUserDetails principal =
        new CebosUserDetails(
            PrincipalKind.BANK_ADMIN,
            1L,
            null,
            List.of(new SimpleGrantedAuthority(BankAdminRole.SUPER_ADMIN.authority())));
    return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
  }
}
