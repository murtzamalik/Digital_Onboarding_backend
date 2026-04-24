package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.bbs")
public record BbsKycProperties(
    String baseUrl,
    double faceMatchMinSimilarity,
    int connectTimeoutSeconds,
    int readTimeoutSeconds) {

  public BbsKycProperties {
    if (faceMatchMinSimilarity < 0 || faceMatchMinSimilarity > 100) {
      throw new IllegalArgumentException("cebos.bbs.face-match-min-similarity must be 0..100");
    }
  }
}
