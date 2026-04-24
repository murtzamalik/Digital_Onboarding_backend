package com.bank.cebos.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Request/response payload logging. Large bodies are capped by {@link #maxLoggedBodyChars}; JSON
 * string values can be truncated per {@link #maxJsonScalarChars} (0 or negative = log full
 * scalars, still subject to maxLoggedBodyChars on the final string).
 */
@Validated
@ConfigurationProperties(prefix = "cebos.http-logging")
public class HttpLoggingProperties {

  private boolean enabled = true;

  /** URI prefixes for which the filter does nothing (e.g. actuator). */
  private List<String> excludedPathPrefixes = new ArrayList<>(List.of("/actuator"));

  /** Max bytes buffered for inbound request/response bodies in the servlet wrappers. */
  private int maxCachedBodyBytes = 10 * 1024 * 1024;

  /** Max characters written per body to the log (after JSON formatting if applicable). */
  private int maxLoggedBodyChars = 2 * 1024 * 1024;

  /**
   * When positive, JSON string values longer than this are replaced with a short preview + length
   * in structured logs. When 0 or negative, scalars are not shortened (only maxLoggedBodyChars
   * applies).
   */
  private int maxJsonScalarChars = 0;

  /** Log full BBS outbound HTTP request/response bodies (subject to caps above). */
  private boolean logBbsHttp = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getExcludedPathPrefixes() {
    return excludedPathPrefixes;
  }

  public void setExcludedPathPrefixes(List<String> excludedPathPrefixes) {
    this.excludedPathPrefixes =
        excludedPathPrefixes == null ? new ArrayList<>() : new ArrayList<>(excludedPathPrefixes);
  }

  public int getMaxCachedBodyBytes() {
    return maxCachedBodyBytes;
  }

  public void setMaxCachedBodyBytes(int maxCachedBodyBytes) {
    this.maxCachedBodyBytes = maxCachedBodyBytes;
  }

  public int getMaxLoggedBodyChars() {
    return maxLoggedBodyChars;
  }

  public void setMaxLoggedBodyChars(int maxLoggedBodyChars) {
    this.maxLoggedBodyChars = maxLoggedBodyChars;
  }

  public int getMaxJsonScalarChars() {
    return maxJsonScalarChars;
  }

  public void setMaxJsonScalarChars(int maxJsonScalarChars) {
    this.maxJsonScalarChars = maxJsonScalarChars;
  }

  public boolean isLogBbsHttp() {
    return logBbsHttp;
  }

  public void setLogBbsHttp(boolean logBbsHttp) {
    this.logBbsHttp = logBbsHttp;
  }
}
