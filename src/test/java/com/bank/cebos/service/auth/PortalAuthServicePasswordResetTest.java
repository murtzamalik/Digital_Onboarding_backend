package com.bank.cebos.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.config.OutboxProperties;
import com.bank.cebos.config.PasswordResetProperties;
import com.bank.cebos.dto.auth.ForgotPasswordRequest;
import com.bank.cebos.dto.auth.ResetPasswordRequest;
import com.bank.cebos.entity.CorporateUser;
import com.bank.cebos.entity.PortalPasswordResetToken;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.AuthRefreshTokenRepository;
import com.bank.cebos.repository.CorporateUserRepository;
import com.bank.cebos.repository.PortalPasswordResetTokenRepository;
import com.bank.cebos.service.audit.PortalUserAuditService;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PortalAuthServicePasswordResetTest {

  @Mock private CorporateUserRepository corporateUserRepository;
  @Mock private AuthRefreshTokenRepository authRefreshTokenRepository;
  @Mock private PortalPasswordResetTokenRepository portalPasswordResetTokenRepository;
  @Mock private PortalPasswordResetNotifier portalPasswordResetNotifier;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenService jwtTokenService;
  @Mock private PortalUserAuditService portalUserAuditService;
  @Mock private OutboxService outboxService;
  @Mock private OutboxProperties outboxProperties;

  private RefreshTokenHasher refreshTokenHasher;
  private PortalAuthService portalAuthService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    refreshTokenHasher = new RefreshTokenHasher();
    objectMapper = new ObjectMapper();
    lenient().when(outboxProperties.isEnabled()).thenReturn(true);
    PasswordResetProperties resetProps = new PasswordResetProperties();
    resetProps.setPublicPortalBaseUrl("http://localhost:5173");
    resetProps.setTokenValidityHours(24);
    JwtProperties jwtProperties = new JwtProperties("cebos", 15, 7, "unit-test-secret-32chars-min!!");
    portalAuthService =
        new PortalAuthService(
            corporateUserRepository,
            authRefreshTokenRepository,
            portalPasswordResetTokenRepository,
            portalPasswordResetNotifier,
            outboxService,
            outboxProperties,
            objectMapper,
            passwordEncoder,
            jwtTokenService,
            refreshTokenHasher,
            jwtProperties,
            resetProps,
            portalUserAuditService);
  }

  @Test
  void forgotPasswordSendsWhenSingleActiveUser() {
    CorporateUser user = org.mockito.Mockito.mock(CorporateUser.class);
    when(user.getId()).thenReturn(9L);
    when(user.getEmail()).thenReturn("u@example.com");
    when(user.getStatus()).thenReturn("ACTIVE");
    when(corporateUserRepository.findByEmailIgnoreCase("u@example.com")).thenReturn(List.of(user));
    when(portalPasswordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    portalAuthService.requestPasswordReset(
        new ForgotPasswordRequest("u@example.com"), "203.0.113.1", "cid-1");

    verify(portalPasswordResetTokenRepository).supersedeUnusedForUser(eq(9L), any(Instant.class));
    verify(portalUserAuditService).recordPasswordResetRequested(9L, "203.0.113.1", "cid-1");
    verify(outboxService)
        .enqueue(
            eq(OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL),
            eq("CorporateUser"),
            eq("9"),
            anyString());
    verify(portalPasswordResetNotifier, never()).sendResetLink(any(), any(), any());
    ArgumentCaptor<PortalPasswordResetToken> captor =
        ArgumentCaptor.forClass(PortalPasswordResetToken.class);
    verify(portalPasswordResetTokenRepository).save(captor.capture());
    assertThat(captor.getValue().getTokenHash()).hasSize(64);
  }

  @Test
  void forgotPasswordNoOpWhenEmailAmbiguous() {
    CorporateUser a = org.mockito.Mockito.mock(CorporateUser.class);
    CorporateUser b = org.mockito.Mockito.mock(CorporateUser.class);
    when(corporateUserRepository.findByEmailIgnoreCase("u@example.com")).thenReturn(List.of(a, b));

    portalAuthService.requestPasswordReset(
        new ForgotPasswordRequest("u@example.com"), "203.0.113.1", "cid-1");

    verify(portalPasswordResetTokenRepository, never()).save(any());
    verify(outboxService, never()).enqueue(anyString(), anyString(), anyString(), anyString());
    verify(portalPasswordResetNotifier, never()).sendResetLink(any(), any(), any());
  }

  @Test
  void forgotPasswordUsesNotifierWhenOutboxDisabled() {
    when(outboxProperties.isEnabled()).thenReturn(false);
    CorporateUser user = org.mockito.Mockito.mock(CorporateUser.class);
    when(user.getId()).thenReturn(9L);
    when(user.getEmail()).thenReturn("u@example.com");
    when(user.getStatus()).thenReturn("ACTIVE");
    when(corporateUserRepository.findByEmailIgnoreCase("u@example.com")).thenReturn(List.of(user));
    when(portalPasswordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    portalAuthService.requestPasswordReset(
        new ForgotPasswordRequest("u@example.com"), "203.0.113.1", "cid-1");

    verify(portalPasswordResetNotifier).sendResetLink(eq("u@example.com"), any(), eq("cid-1"));
    verify(outboxService, never()).enqueue(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void resetPasswordUpdatesHashAndRevokesRefresh() {
    String raw = refreshTokenHasher.newOpaqueRefreshToken();
    String hash = refreshTokenHasher.sha256Hex(raw);
    PortalPasswordResetToken row =
        new PortalPasswordResetToken(
            9L, hash, Instant.now().plus(1, ChronoUnit.HOURS), null, null);
    when(portalPasswordResetTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(row));
    CorporateUser user = org.mockito.Mockito.mock(CorporateUser.class);
    when(user.getId()).thenReturn(9L);
    when(user.getStatus()).thenReturn("ACTIVE");
    when(corporateUserRepository.findById(9L)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-secret-99")).thenReturn("ENC");

    portalAuthService.resetPassword(new ResetPasswordRequest(raw, "new-secret-99"));

    verify(user).setPasswordHash("ENC");
    verify(corporateUserRepository).save(user);
    verify(portalPasswordResetTokenRepository).save(row);
    assertThat(row.getUsedAt()).isNotNull();
    verify(authRefreshTokenRepository).revokeAllActiveForPrincipal(PrincipalKind.PORTAL, 9L);
    verify(portalUserAuditService).recordPasswordResetCompleted(9L);
  }

  @Test
  void resetPasswordRejectsUsedToken() {
    String raw = refreshTokenHasher.newOpaqueRefreshToken();
    String hash = refreshTokenHasher.sha256Hex(raw);
    PortalPasswordResetToken row =
        new PortalPasswordResetToken(
            9L, hash, Instant.now().plus(1, ChronoUnit.HOURS), null, null);
    row.setUsedAt(Instant.now());
    when(portalPasswordResetTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(row));

    assertThatThrownBy(
            () -> portalAuthService.resetPassword(new ResetPasswordRequest(raw, "new-secret-99")))
        .isInstanceOf(BadCredentialsException.class);

    verify(corporateUserRepository, never()).save(any());
  }
}
