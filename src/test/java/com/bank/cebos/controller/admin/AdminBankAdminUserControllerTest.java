package com.bank.cebos.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.dto.admin.BankAdminUserResponse;
import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
import com.bank.cebos.service.admin.AdminBankAdminUserService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminBankAdminUserController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class AdminBankAdminUserControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockBean private AdminBankAdminUserService adminBankAdminUserService;

  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean private RedisMinuteWindowLimiter redisMinuteWindowLimiter;

  @MockBean private UploadBatchRepository uploadBatchRepository;

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

  private static UsernamePasswordAuthenticationToken superAdminAuth() {
    CebosUserDetails principal =
        new CebosUserDetails(
            PrincipalKind.BANK_ADMIN,
            1L,
            null,
            List.of(new SimpleGrantedAuthority(BankAdminRole.SUPER_ADMIN.authority())));
    return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
  }

  @Test
  void listReturns200() throws Exception {
    when(adminBankAdminUserService.list(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(get("/api/v1/admin/bank-admin-users").with(authentication(superAdminAuth())))
        .andExpect(status().isOk());
  }

  @Test
  void patchPasswordReturnsUpdatedUser() throws Exception {
    when(adminBankAdminUserService.updatePassword(eq(9L), eq("newpass-99")))
        .thenReturn(new BankAdminUserResponse(9L, "u@test", "U", "VIEWER", "ACTIVE"));

    mockMvc
        .perform(
            patch("/api/v1/admin/bank-admin-users/9/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("newPassword", "newpass-99")))
                .with(authentication(superAdminAuth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.email").value("u@test"));
  }
}
