package com.bank.cebos.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

  private static final HexFormat HEX = HexFormat.of();
  private final SecureRandom secureRandom = new SecureRandom();

  public String newOpaqueRefreshToken() {
    byte[] buf = new byte[32];
    secureRandom.nextBytes(buf);
    return HEX.formatHex(buf);
  }

  public String sha256Hex(String rawToken) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
