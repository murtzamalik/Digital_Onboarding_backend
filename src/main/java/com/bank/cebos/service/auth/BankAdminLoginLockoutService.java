package com.bank.cebos.service.auth;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * v2-style admin login protection: after {@value #MAX_FAILURES} failed password attempts within a
 * {@value #FAILURE_WINDOW_MINUTES}-minute window, block further attempts for {@value #LOCK_MINUTES} minutes.
 * Uses Redis; fails open if Redis is unavailable (same policy as {@link
 * com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter}).
 */
@Service
public class BankAdminLoginLockoutService {

  private static final Logger log = LoggerFactory.getLogger(BankAdminLoginLockoutService.class);

  static final int MAX_FAILURES = 5;
  static final int FAILURE_WINDOW_MINUTES = 15;
  static final int LOCK_MINUTES = 30;

  private static final String FAIL_PREFIX = "cebos:admin-login-fail:";
  private static final String LOCK_PREFIX = "cebos:admin-login-lock:";

  private final StringRedisTemplate redisTemplate;

  public BankAdminLoginLockoutService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean isLocked(String email) {
    String key = LOCK_PREFIX + normalize(email);
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    } catch (Exception e) {
      log.warn("Admin lockout check failed (fail-open): {}", e.toString());
      return false;
    }
  }

  /** Call after a failed password attempt for an existing bank admin account (known email). */
  public void recordFailure(String email) {
    String norm = normalize(email);
    String failKey = FAIL_PREFIX + norm;
    String lockKey = LOCK_PREFIX + norm;
    try {
      Long n = redisTemplate.opsForValue().increment(failKey);
      if (n != null && n == 1L) {
        redisTemplate.expire(failKey, Duration.ofMinutes(FAILURE_WINDOW_MINUTES));
      }
      if (n != null && n >= MAX_FAILURES) {
        redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(LOCK_MINUTES));
        redisTemplate.delete(failKey);
      }
    } catch (Exception e) {
      log.warn("Admin lockout record failed (ignored): {}", e.toString());
    }
  }

  public void clearOnSuccess(String email) {
    String norm = normalize(email);
    try {
      redisTemplate.delete(FAIL_PREFIX + norm);
      redisTemplate.delete(LOCK_PREFIX + norm);
    } catch (Exception e) {
      log.warn("Admin lockout clear failed (ignored): {}", e.toString());
    }
  }

  private static String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }
}
