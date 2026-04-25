package com.bank.cebos.util;

/** Shared masking for exports and portal list/detail (last N characters visible). */
public final class PiiMasking {

  private PiiMasking() {}

  public static String maskTail(String value, int visible) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String t = value.trim();
    if (t.length() <= visible) {
      return "****";
    }
    return "*".repeat(t.length() - visible) + t.substring(t.length() - visible);
  }
}
