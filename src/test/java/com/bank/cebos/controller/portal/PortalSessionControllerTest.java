package com.bank.cebos.controller.portal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalSessionController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class PortalSessionControllerTest {

  @Autowired private MockMvc mockMvc;

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
  void sessionReturnsIds() throws Exception {
    mockMvc
        .perform(get("/api/v1/portal/session").with(authentication(portalAuth(7L, 9L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portalUserId").value(7))
        .andExpect(jsonPath("$.corporateClientId").value(9))
        .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"));
  }

  private static UsernamePasswordAuthenticationToken portalAuth(long userId, long clientId) {
    CebosUserDetails details =
        new CebosUserDetails(
            PrincipalKind.PORTAL,
            userId,
            clientId,
            List.of(new SimpleGrantedAuthority(PortalRole.ADMIN.authority())));
    return new UsernamePasswordAuthenticationToken(details, "n/a", details.authorities());
  }
}
