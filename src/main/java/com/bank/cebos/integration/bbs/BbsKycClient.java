package com.bank.cebos.integration.bbs;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * KYC BBS (Biometric / document SDK) HTTP API — Pakistani CNIC OCR and face comparison.
 */
public interface BbsKycClient {

  /** POST /api/extractPakistaniIdCard with {@code { "base64Image": "..." }} */
  JsonNode extractPakistaniIdCard(String base64Image);

  /** POST /api/match with id card + selfie JPEG base64. */
  BbsFaceMatchResult matchIdCardToSelfie(String idCardBase64, String selfieBase64);
}
