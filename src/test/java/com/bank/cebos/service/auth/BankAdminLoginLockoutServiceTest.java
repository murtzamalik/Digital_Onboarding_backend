package com.bank.cebos.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class BankAdminLoginLockoutServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;

  private BankAdminLoginLockoutService service;

  @BeforeEach
  void setUp() {
    service = new BankAdminLoginLockoutService(redisTemplate);
  }

  @Test
  void isLockedWhenKeyPresent() {
    when(redisTemplate.hasKey("cebos:admin-login-lock:admin@test.local")).thenReturn(true);
    assertThat(service.isLocked("Admin@Test.Local")).isTrue();
  }

  @Test
  void fifthFailureSetsLockAndClearsCounter() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.increment("cebos:admin-login-fail:admin@test.local")).thenReturn(5L);

    service.recordFailure("admin@test.local");

    verify(valueOps).set(eq("cebos:admin-login-lock:admin@test.local"), eq("1"), eq(Duration.ofMinutes(30)));
    verify(redisTemplate).delete("cebos:admin-login-fail:admin@test.local");
  }

  @Test
  void firstFailureSetsExpiryOnly() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.increment("cebos:admin-login-fail:a@b.c")).thenReturn(1L);

    service.recordFailure("a@b.c");

    verify(redisTemplate).expire(eq("cebos:admin-login-fail:a@b.c"), eq(Duration.ofMinutes(15)));
    verify(valueOps, never()).set(any(), any(), any(Duration.class));
  }

  @Test
  void clearOnSuccessDeletesKeys() {
    service.clearOnSuccess("x@y.z");
    verify(redisTemplate).delete("cebos:admin-login-fail:x@y.z");
    verify(redisTemplate).delete("cebos:admin-login-lock:x@y.z");
  }
}
