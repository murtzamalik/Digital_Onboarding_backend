package com.bank.cebos.util;

import java.util.Base64;
import org.springframework.util.StringUtils;

public final class Base64ImagePayload {

  private static final int MAX_DECODED_BYTES = 12 * 1024 * 1024;

  private Base64ImagePayload() {}

  /** Strips {@code data:image/...;base64,} and whitespace; returns raw base64 for API use. */
  public static String normalizeForWire(String input) {
    if (!StringUtils.hasText(input)) {
      return "";
    }
    String t = input.trim();
    int comma = t.indexOf("base64,");
    if (comma > 0) {
      t = t.substring(comma + "base64,".length());
    }
    return t.replaceAll("\\s", "");
  }

  public static byte[] decodeToBytes(String input) {
    String normalized = normalizeForWire(input);
    if (!StringUtils.hasText(normalized)) {
      throw new IllegalArgumentException("Image data is empty");
    }
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid base64 image payload", e);
    }
    if (decoded.length == 0) {
      throw new IllegalArgumentException("Decoded image is empty");
    }
    if (decoded.length > MAX_DECODED_BYTES) {
      throw new IllegalArgumentException("Image exceeds maximum size");
    }
    return decoded;
  }
}
