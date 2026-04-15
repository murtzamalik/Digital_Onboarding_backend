package com.bank.cebos.security.ratelimit;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed window per UTC minute. On Redis errors, returns {@code true} (fail-open) and logs a warning.
 */
@Component
public class RedisMinuteWindowLimiter {

  private static final Logger log = LoggerFactory.getLogger(RedisMinuteWindowLimiter.class);

  private final StringRedisTemplate redisTemplate;

  public RedisMinuteWindowLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * @return {@code true} if request is allowed, {@code false} if rate limit exceeded
   */
  public boolean tryConsume(String scope, String clientKey, int maxPerMinute) {
    if (maxPerMinute <= 0) {
      return true;
    }
    long minute = Instant.now().getEpochSecond() / 60;
    String redisKey = "cebos:rl:" + scope + ":" + clientKey + ":" + minute;
    try {
      Long n = redisTemplate.opsForValue().increment(redisKey);
      if (n != null && n == 1L) {
        redisTemplate.expire(redisKey, Duration.ofMinutes(2));
      }
      return n == null || n <= maxPerMinute;
    } catch (Exception e) {
      log.warn("Rate limit check failed (fail-open): {}", e.toString());
      return true;
    }
  }
}
