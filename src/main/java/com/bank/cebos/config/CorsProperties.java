package com.bank.cebos.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cebos.cors")
public class CorsProperties {

  /**
   * CORS {@code Access-Control-Allow-Origin} patterns (Spring supports {@code *} in host/port
   * segments). Override via {@code cebos.cors.allowed-origin-patterns} or env for additional
   * deploy hosts.
   */
  private List<String> allowedOriginPatterns =
      List.of(
          "http://localhost:*",
          "http://127.0.0.1:*",
          "http://46.224.146.158:*",
          "https://46.224.146.158:*");

  public List<String> getAllowedOriginPatterns() {
    return allowedOriginPatterns;
  }

  public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = allowedOriginPatterns;
  }
}
