package com.bank.cebos.service.auth;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.dto.auth.LoginRequest;
import com.bank.cebos.dto.auth.RefreshRequest;
import com.bank.cebos.dto.auth.TokenResponse;
import com.bank.cebos.entity.AuthRefreshToken;
import com.bank.cebos.entity.BankAdminUser;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.AuthRefreshTokenRepository;
import com.bank.cebos.repository.BankAdminUserRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BankAdminAuthService {

  private static final String STATUS_ACTIVE = "ACTIVE";

  private final BankAdminUserRepository bankAdminUserRepository;
  private final AuthRefreshTokenRepository authRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenHasher refreshTokenHasher;
  private final JwtProperties jwtProperties;

  public BankAdminAuthService(
      BankAdminUserRepository bankAdminUserRepository,
      AuthRefreshTokenRepository authRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      RefreshTokenHasher refreshTokenHasher,
      JwtProperties jwtProperties) {
    this.bankAdminUserRepository = bankAdminUserRepository;
    this.authRefreshTokenRepository = authRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenHasher = refreshTokenHasher;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    BankAdminUser user =
        bankAdminUserRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!STATUS_ACTIVE.equals(user.getStatus())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return issueTokens(user);
  }

  @Transactional
  public TokenResponse refresh(RefreshRequest request) {
    String hash = refreshTokenHasher.sha256Hex(request.refreshToken());
    AuthRefreshToken row =
        authRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    validateRefreshRow(row, PrincipalKind.BANK_ADMIN);
    BankAdminUser user =
        bankAdminUserRepository
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

  private TokenResponse issueTokens(BankAdminUser user) {
    String access =
        jwtTokenService.issueAccessToken(
            PrincipalKind.BANK_ADMIN,
            user.getId(),
            null,
            user.getRole(),
            "/api/v1/admin");
    String refreshPlain = refreshTokenHasher.newOpaqueRefreshToken();
    String tokenHash = refreshTokenHasher.sha256Hex(refreshPlain);
    Instant expiresAt =
        Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS);
    authRefreshTokenRepository.save(
        new AuthRefreshToken(
            tokenHash, PrincipalKind.BANK_ADMIN, user.getId(), null, expiresAt));
    return new TokenResponse(access, refreshPlain);
  }
}
