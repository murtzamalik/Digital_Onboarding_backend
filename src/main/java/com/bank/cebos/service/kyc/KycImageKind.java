package com.bank.cebos.service.kyc;

public enum KycImageKind {
  CNIC_FRONT,
  CNIC_BACK,
  SELFIE;

  public static KycImageKind fromPathSegment(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("kind required");
    }
    return switch (raw.trim().toLowerCase()) {
      case "front", "cnic-front", "cnic_front" -> CNIC_FRONT;
      case "back", "cnic-back", "cnic_back" -> CNIC_BACK;
      case "selfie" -> SELFIE;
      default -> throw new IllegalArgumentException("Unknown image kind: " + raw);
    };
  }
}
