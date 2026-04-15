package com.bank.cebos.util;

/**
 * Minimal Pakistan-oriented mobile normalization for SMS payloads. Product may replace with libphonenumber.
 */
public final class PhoneE164 {

  private PhoneE164() {}

  public static String toE164(String mobile) {
    if (mobile == null) {
      return null;
    }
    String m = mobile.trim().replace(" ", "");
    if (m.isEmpty()) {
      return null;
    }
    if (m.startsWith("+")) {
      return m;
    }
    if (m.startsWith("0") && m.length() >= 10) {
      return "+92" + m.substring(1);
    }
    if (m.startsWith("92") && m.length() >= 11) {
      return "+" + m;
    }
    return "+92" + m;
  }
}
