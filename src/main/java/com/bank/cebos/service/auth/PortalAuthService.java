package com.bank.cebos.service.auth;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.config.OutboxProperties;
import com.bank.cebos.config.PasswordResetProperties;
import com.bank.cebos.dto.auth.ForgotPasswordRequest;
import com.bank.cebos.dto.auth.ForgotPasswordResponse;
import com.bank.cebos.dto.auth.LoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.ResetPasswordRequest;
import com.bank.cebos.dto.auth.ResetPasswordResponse;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.entity.AuthRefreshToken;
import com.bank.cebos.entity.CorporateUser;
import com.bank.cebos.entity.PortalPasswordResetToken;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.AuthRefreshTokenRepository;
import com.bank.cebos.repository.CorporateUserRepository;
import com.bank.cebos.repository.PortalPasswordResetTokenRepository;
import com.bank.cebos.service.audit.PortalUserAuditService;
import com.bank.cebos.service.outbox.OutboxEventType;
import com.bank.cebos.service.outbox.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PortalAuthService {

  private static final Logger log = LoggerFactory.getLogger(PortalAuthService.class);

  private static final String STATUS_ACTIVE = "ACTIVE";

  private final CorporateUserRepository corporateUserRepository;
  private final AuthRefreshTokenRepository authRefreshTokenRepository;
  private final PortalPasswordResetTokenRepository portalPasswordResetTokenRepository;
  private final PortalPasswordResetNotifier portalPasswordResetNotifier;
  private final OutboxService outboxService;
  private final OutboxProperties outboxProperties;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenHasher refreshTokenHasher;
  private final JwtProperties jwtProperties;
  private final PasswordResetProperties passwordResetProperties;
  private final PortalUserAuditService portalUserAuditService;

  public PortalAuthService(
      CorporateUserRepository corporateUserRepository,
      AuthRefreshTokenRepository authRefreshTokenRepository,
      PortalPasswordResetTokenRepository portalPasswordResetTokenRepository,
      PortalPasswordResetNotifier portalPasswordResetNotifier,
      OutboxService outboxService,
      OutboxProperties outboxProperties,
      ObjectMapper objectMapper,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      RefreshTokenHasher refreshTokenHasher,
      JwtProperties jwtProperties,
      PasswordResetProperties passwordResetProperties,
      PortalUserAuditService portalUserAuditService) {
    this.corporateUserRepository = corporateUserRepository;
    this.authRefreshTokenRepository = authRefreshTokenRepository;
    this.portalPasswordResetTokenRepository = portalPasswordResetTokenRepository;
    this.portalPasswordResetNotifier = portalPasswordResetNotifier;
    this.outboxService = outboxService;
    this.outboxProperties = outboxProperties;
    this.objectMapper = objectMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenHasher = refreshTokenHasher;
    this.jwtProperties = jwtProperties;
    this.passwordResetProperties = passwordResetProperties;
    this.portalUserAuditService = portalUserAuditService;
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    List<CorporateUser> matches = corporateUserRepository.findByEmailIgnoreCase(request.email());
    if (matches.size() != 1) {
      throw new BadCredentialsException("Invalid credentials");
    }
    CorporateUser user = matches.get(0);
    if (!STATUS_ACTIVE.equals(user.getStatus())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return issueTokens(user);
  }

  @Transactional
  public ForgotPasswordResponse requestPasswordReset(
      ForgotPasswordRequest request, String requestIp, String correlationId) {
    List<CorporateUser> matches = corporateUserRepository.findByEmailIgnoreCase(request.email());
    if (matches.size() != 1 || !STATUS_ACTIVE.equals(matches.get(0).getStatus())) {
      return ForgotPasswordResponse.acknowledged();
    }
    CorporateUser user = matches.get(0);
    Instant now = Instant.now();
    portalPasswordResetTokenRepository.supersedeUnusedForUser(user.getId(), now);
    String rawToken = refreshTokenHasher.newOpaqueRefreshToken();
    String hash = refreshTokenHasher.sha256Hex(rawToken);
    Instant expiresAt =
        now.plus(passwordResetProperties.getTokenValidityHours(), ChronoUnit.HOURS);
    portalPasswordResetTokenRepository.save(
        new PortalPasswordResetToken(
            user.getId(), hash, expiresAt, truncateIp(requestIp), correlationId));
    portalUserAuditService.recordPasswordResetRequested(
        user.getId(), truncateIp(requestIp), correlationId);
    dispatchPasswordResetEmail(user, rawToken, correlationId);
    return ForgotPasswordResponse.acknowledged();
  }

  private void dispatchPasswordResetEmail(
      CorporateUser user, String rawToken, String correlationId) {
    if (outboxProperties.isEnabled()) {
      try {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("recipientEmail", user.getEmail());
        payload.put("rawToken", rawToken);
        payload.put("correlationId", correlationId != null ? correlationId : "");
        outboxService.enqueue(
            OutboxEventType.PORTAL_PASSWORD_RESET_EMAIL,
            "CorporateUser",
            String.valueOf(user.getId()),
            objectMapper.writeValueAsString(payload));
      } catch (JsonProcessingException e) {
        log.warn(
            "Failed to serialize password-reset outbox payload for user {}; sending synchronously",
            user.getId(),
            e);
        portalPasswordResetNotifier.sendResetLink(
            user.getEmail(), rawToken, correlationId);
      }
    } else {
      portalPasswordResetNotifier.sendResetLink(user.getEmail(), rawToken, correlationId);
    }
  }

  @Transactional
  public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
    String hash = refreshTokenHasher.sha256Hex(request.token());
    PortalPasswordResetToken row =
        portalPasswordResetTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(
                () -> new BadCredentialsException("Invalid or expired password reset link"));
    if (row.getUsedAt() != null || row.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Invalid or expired password reset link");
    }
    CorporateUser user =
        corporateUserRepository
            .findById(row.getCorporateUserId())
            .orElseThrow(() -> new BadCredentialsException("Invalid or expired password reset link"));
    if (!STATUS_ACTIVE.equals(user.getStatus())) {
      throw new BadCredentialsException("Invalid or expired password reset link");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    corporateUserRepository.save(user);
    row.setUsedAt(Instant.now());
    portalPasswordResetTokenRepository.save(row);
    authRefreshTokenRepository.revokeAllActiveForPrincipal(PrincipalKind.PORTAL, user.getId());
    portalUserAuditService.recordPasswordResetCompleted(user.getId());
    return ResetPasswordResponse.ok();
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) {
    String hash = refreshTokenHasher.sha256Hex(request.refreshToken());
    AuthRefreshToken row =
        authRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    validateRefreshRow(row, PrincipalKind.PORTAL);
    CorporateUser user =
        corporateUserRepository
            .findById(row.getPrincipalId())
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (!STATUS_ACTIVE.equals(user.getStatus())) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    row.setRevoked(true);
    authRefreshTokenRepository.save(row);
    return issueTokens(user);
  }

  private void validateRefreshRow(AuthRefreshToken row, PrincipalKind expectedKind) {
    if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    if (row.getPrincipalKind() != expectedKind) {
      throw new BadCredentialsException("Invalid refresh token");
    }
  }

  private TokenResponse issueTokens(CorporateUser user) {
    String access =
        jwtTokenService.issueAccessToken(
            PrincipalKind.PORTAL,
            user.getId(),
            user.getCorporateClientId(),
            user.getRole(),
            "/api/v1/portal");
    String refreshPlain = refreshTokenHasher.newOpaqueRefreshToken();
    String hash = refreshTokenHasher.sha256Hex(refreshPlain);
    Instant expiresAt =
        Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS);
    authRefreshTokenRepository.save(
        new AuthRefreshToken(
            hash,
            PrincipalKind.PORTAL,
            user.getId(),
            user.getCorporateClientId(),
            expiresAt));
    return new TokenResponse(access, refreshPlain);
  }

  private static String truncateIp(String requestIp) {
    if (requestIp == null || requestIp.isBlank()) {
      return null;
    }
    return requestIp.length() > 64 ? requestIp.substring(0, 64) : requestIp;
  }
}
