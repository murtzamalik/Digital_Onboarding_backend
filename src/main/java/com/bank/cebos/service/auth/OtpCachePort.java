package com.bank.cebos.service.auth;

import java.time.Duration;
import java.util.Optional;

public interface OtpCachePort {
  Optional<String> readHash(Long employeeOnboardingId);

  void writeHash(Long employeeOnboardingId, String otpHash, Duration ttl);

  void deleteHash(Long employeeOnboardingId);
}
