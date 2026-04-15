package com.bank.cebos.service.auth;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpCacheStore implements OtpCachePort {

  private static final Logger log = LoggerFactory.getLogger(OtpCacheStore.class);

  private final StringRedisTemplate stringRedisTemplate;

  public OtpCacheStore(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public Optional<String> readHash(Long employeeOnboardingId) {
    try {
      return Optional.ofNullable(stringRedisTemplate.opsForValue().get(redisKey(employeeOnboardingId)));
    } catch (RuntimeException e) {
      log.warn("Redis unavailable while reading OTP cache. Falling back to DB-only verification.", e);
      return Optional.empty();
    }
  }

  @Override
  public void writeHash(Long employeeOnboardingId, String otpHash, Duration ttl) {
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    try {
      stringRedisTemplate.opsForValue().set(redisKey(employeeOnboardingId), otpHash, ttl);
    } catch (RuntimeException e) {
      log.warn("Redis unavailable while storing OTP cache. Continuing with DB-only path.", e);
    }
  }

  @Override
  public void deleteHash(Long employeeOnboardingId) {
    try {
      stringRedisTemplate.delete(redisKey(employeeOnboardingId));
    } catch (RuntimeException e) {
      log.warn("Redis unavailable while deleting OTP cache key. Continuing with DB-only path.", e);
    }
  }

  private String redisKey(Long employeeOnboardingId) {
    return "otp:employee:" + employeeOnboardingId;
  }
}
