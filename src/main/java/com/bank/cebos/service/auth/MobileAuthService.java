package com.bank.cebos.service.auth;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.dto.auth.MobileLoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.entity.AuthRefreshToken;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.AuthRefreshTokenRepository;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MobileAuthService {

  private final EmployeeOnboardingService employeeOnboardingService;
  private final AuthRefreshTokenRepository authRefreshTokenRepository;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenHasher refreshTokenHasher;
  private final JwtProperties jwtProperties;
  private final String mobileDevSecret;

  public MobileAuthService(
      EmployeeOnboardingService employeeOnboardingService,
      AuthRefreshTokenRepository authRefreshTokenRepository,
      JwtTokenService jwtTokenService,
      RefreshTokenHasher refreshTokenHasher,
      JwtProperties jwtProperties,
      @Value("${cebos.mobile.dev-secret:}") String mobileDevSecret) {
    this.employeeOnboardingService = employeeOnboardingService;
    this.authRefreshTokenRepository = authRefreshTokenRepository;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenHasher = refreshTokenHasher;
    this.jwtProperties = jwtProperties;
    this.mobileDevSecret = mobileDevSecret == null ? "" : mobileDevSecret;
  }

  @Transactional
  public TokenResponse login(MobileLoginRequest request, String devSecretHeader) {
    if (mobileDevSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Passwordless mobile login (OTP) is not implemented. For local development only, set"
              + " cebos.mobile.dev-secret or CEBOS_MOBILE_DEV_SECRET and send header"
              + " X-CEBOS-Mobile-Dev-Secret.");
    }
    if (devSecretHeader == null || !mobileDevSecret.equals(devSecretHeader)) {
      throw new BadCredentialsException("Invalid mobile dev credentials");
    }
    EmployeeOnboarding employee =
        employeeOnboardingService.requireByEmployeeRefForMobileLogin(request.employeeRef());
    return issueTokens(employee);
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) {
    String hash = refreshTokenHasher.sha256Hex(request.refreshToken());
    AuthRefreshToken row =
        authRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    validateRefreshRow(row, PrincipalKind.MOBILE);
    EmployeeOnboarding employee =
        employeeOnboardingService.requireByIdForMobileRefresh(row.getPrincipalId());
    row.setRevoked(true);
    authRefreshTokenRepository.save(row);
    return issueTokens(employee);
  }

  private void validateRefreshRow(AuthRefreshToken row, PrincipalKind expectedKind) {
    if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    if (row.getPrincipalKind() != expectedKind) {
      throw new BadCredentialsException("Invalid refresh token");
    }
  }

  private TokenResponse issueTokens(EmployeeOnboarding employee) {
    String access =
        jwtTokenService.issueAccessToken(
            PrincipalKind.MOBILE,
            employee.getId(),
            employee.getCorporateClientId(),
            "",
            "/api/v1/mobile");
    String refreshPlain = refreshTokenHasher.newOpaqueRefreshToken();
    String tokenHash = refreshTokenHasher.sha256Hex(refreshPlain);
    Instant expiresAt =
        Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS);
    authRefreshTokenRepository.save(
        new AuthRefreshToken(
            tokenHash,
            PrincipalKind.MOBILE,
            employee.getId(),
            employee.getCorporateClientId(),
            expiresAt));
    return new TokenResponse(access, refreshPlain);
  }
}
