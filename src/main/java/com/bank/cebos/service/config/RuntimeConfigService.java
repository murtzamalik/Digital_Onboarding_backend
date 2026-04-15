package com.bank.cebos.service.config;

import com.bank.cebos.entity.SystemConfiguration;
import com.bank.cebos.repository.SystemConfigurationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RuntimeConfigService {

  private static final Duration CACHE_TTL = Duration.ofMinutes(5);
  private static final Map<String, Boolean> TRUE_VALUES =
      Map.of("true", true, "1", true, "yes", true, "y", true, "on", true);
  private static final Map<String, Boolean> FALSE_VALUES =
      Map.of("false", false, "0", false, "no", false, "n", false, "off", false);

  private final SystemConfigurationRepository systemConfigurationRepository;
  private final ConcurrentHashMap<String, CachedValue> cache = new ConcurrentHashMap<>();

  public RuntimeConfigService(SystemConfigurationRepository systemConfigurationRepository) {
    this.systemConfigurationRepository = systemConfigurationRepository;
  }

  public String getString(String key, String fallback) {
    Optional<String> configured = loadValue(key);
    if (configured.isEmpty() || configured.get().isBlank()) {
      return fallback;
    }
    return configured.get();
  }

  public int getInt(String key, int fallback) {
    Optional<String> configured = loadValue(key);
    if (configured.isEmpty()) {
      return fallback;
    }
    try {
      return Integer.parseInt(configured.get().trim());
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  public boolean getBoolean(String key, boolean fallback) {
    Optional<String> configured = loadValue(key);
    if (configured.isEmpty()) {
      return fallback;
    }
    String normalized = configured.get().trim().toLowerCase(Locale.ROOT);
    if (TRUE_VALUES.containsKey(normalized)) {
      return true;
    }
    if (FALSE_VALUES.containsKey(normalized)) {
      return false;
    }
    return fallback;
  }

  private Optional<String> loadValue(String key) {
    Instant now = Instant.now();
    CachedValue cached = cache.get(key);
    if (cached != null && cached.expiresAt().isAfter(now)) {
      return cached.value();
    }

    Optional<String> value =
        systemConfigurationRepository.findByConfigKey(key).map(SystemConfiguration::getConfigValue);
    cache.put(key, new CachedValue(value, now.plus(CACHE_TTL)));
    return value;
  }

  private record CachedValue(Optional<String> value, Instant expiresAt) {}
}
