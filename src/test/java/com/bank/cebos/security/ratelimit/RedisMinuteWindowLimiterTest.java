package com.bank.cebos.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisMinuteWindowLimiterTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;

  private RedisMinuteWindowLimiter limiter;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    limiter = new RedisMinuteWindowLimiter(redisTemplate);
  }

  @Test
  void allowsWhenUnderCap() {
    when(valueOps.increment(anyString())).thenReturn(1L, 2L, 3L);

    assertThat(limiter.tryConsume("t", "ip1", 5)).isTrue();
    assertThat(limiter.tryConsume("t", "ip1", 5)).isTrue();
    assertThat(limiter.tryConsume("t", "ip1", 5)).isTrue();
    verify(redisTemplate).expire(anyString(), any(Duration.class));
  }

  @Test
  void deniesWhenOverCap() {
    when(valueOps.increment(anyString())).thenReturn(4L);

    assertThat(limiter.tryConsume("t", "ip1", 3)).isFalse();
  }

  @Test
  void maxZeroSkipsRedis() {
    assertThat(limiter.tryConsume("t", "ip1", 0)).isTrue();
    verify(valueOps, never()).increment(anyString());
  }

  @Test
  void failOpenOnRedisError() {
    when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

    assertThat(limiter.tryConsume("t", "ip1", 3)).isTrue();
  }
}
