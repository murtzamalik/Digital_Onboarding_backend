package com.bank.cebos.controller.mobile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.dto.mobile.CnicCaptureRequest;
import com.bank.cebos.dto.mobile.MobileJourneyStatusResponse;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.mobile.MobileJourneyWorkflowService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MobileJourneyController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class MobileJourneyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private MobileJourneyWorkflowService mobileJourneyWorkflowService;
  @MockBean private RuntimeConfigService runtimeConfigService;
  @MockBean private UploadBatchRepository uploadBatchRepository;
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private RedisMinuteWindowLimiter redisMinuteWindowLimiter;

  private final ObjectMapper objectMapper = new ObjectMapper();

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
  void policyIsPublic() throws Exception {
    when(runtimeConfigService.getString("mobile.min_supported_version", "1.0.0")).thenReturn("1.2.0");
    when(runtimeConfigService.getBoolean("mobile.force_update_enabled", false)).thenReturn(true);

    mockMvc
        .perform(get("/api/v1/mobile/policy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.minSupportedVersion").value("1.2.0"))
        .andExpect(jsonPath("$.forceUpdate").value(true));
  }

  @Test
  void statusUsesPrincipalId() throws Exception {
    when(mobileJourneyWorkflowService.status(77L))
        .thenReturn(new MobileJourneyStatusResponse(77L, "E-77", OnboardingStatus.QUIZ_PENDING, 1L));

    mockMvc
        .perform(get("/api/v1/mobile/status").with(authentication(mobileAuth(77L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeOnboardingId").value(77))
        .andExpect(jsonPath("$.status").value("QUIZ_PENDING"));
  }

  @Test
  void cnicFrontDelegates() throws Exception {
    when(mobileJourneyWorkflowService.submitCnicFront(eq(77L), any(CnicCaptureRequest.class)))
        .thenReturn(new MobileJourneyStatusResponse(77L, "E-77", OnboardingStatus.NADRA_PENDING, 1L));

    mockMvc
        .perform(
            post("/api/v1/mobile/kyc/cnic/front")
                .with(authentication(mobileAuth(77L)))
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CnicCaptureRequest("QUJDRA=="))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NADRA_PENDING"));
  }

  private static UsernamePasswordAuthenticationToken mobileAuth(long onboardingId) {
    CebosUserDetails details = new CebosUserDetails(PrincipalKind.MOBILE, onboardingId, 1L, List.of());
    return new UsernamePasswordAuthenticationToken(details, "n/a", details.authorities());
  }
}
