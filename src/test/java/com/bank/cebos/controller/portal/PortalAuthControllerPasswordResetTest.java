package com.bank.cebos.controller.portal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.dto.auth.ForgotPasswordResponse;
import com.bank.cebos.dto.auth.ResetPasswordResponse;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
import com.bank.cebos.service.auth.PortalAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalAuthController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class PortalAuthControllerPasswordResetTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private PortalAuthService portalAuthService;

  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean private RedisMinuteWindowLimiter redisMinuteWindowLimiter;

  @MockBean private UploadBatchRepository uploadBatchRepository;

  @BeforeEach
  void jwtFilterDelegates() throws Exception {
    when(redisMinuteWindowLimiter.tryConsume(anyString(), anyString(), anyInt())).thenReturn(true);
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
  void forgotPasswordReturnsAck() throws Exception {
    when(portalAuthService.requestPasswordReset(any(), any(), any()))
        .thenReturn(ForgotPasswordResponse.acknowledged());

    mockMvc
        .perform(
            post("/api/v1/portal/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists());

    verify(portalAuthService).requestPasswordReset(any(), any(), any());
  }

  @Test
  void resetPasswordReturnsMessage() throws Exception {
    when(portalAuthService.resetPassword(any())).thenReturn(ResetPasswordResponse.ok());

    mockMvc
        .perform(
            post("/api/v1/portal/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new com.bank.cebos.dto.auth.ResetPasswordRequest(
                            "aabbccdd", "new-pass-99"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists());

    verify(portalAuthService).resetPassword(any());
  }
}
