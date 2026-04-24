package com.bank.cebos.integration.bbs;

import com.bank.cebos.config.BbsKycProperties;
import com.bank.cebos.config.HttpLoggingProperties;
import com.bank.cebos.logging.HttpLogPayloadFormatter;
import com.bank.cebos.util.Base64ImagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BbsKycClientImpl implements BbsKycClient {

  private static final Logger log = LoggerFactory.getLogger(BbsKycClientImpl.class);

  private final BbsKycProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpLoggingProperties httpLoggingProperties;
  private final HttpClient httpClient;

  public BbsKycClientImpl(
      BbsKycProperties properties,
      ObjectMapper objectMapper,
      HttpLoggingProperties httpLoggingProperties) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpLoggingProperties = httpLoggingProperties;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(1, properties.connectTimeoutSeconds())))
            .build();
  }

  @Override
  public JsonNode extractPakistaniIdCard(String base64Image) {
    requireBaseUrl();
    String b64 = Base64ImagePayload.normalizeForWire(base64Image);
    if (!StringUtils.hasText(b64)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base64Image is required");
    }
    byte[] body = toJsonBytes(Map.of("base64Image", b64), "extractPakistaniIdCard request");
    return postJsonNode("/api/extractPakistaniIdCard", body, "extractPakistaniIdCard");
  }

  @Override
  public BbsFaceMatchResult matchIdCardToSelfie(String idCardBase64, String selfieBase64) {
    requireBaseUrl();
    String idc = Base64ImagePayload.normalizeForWire(idCardBase64);
    String self = Base64ImagePayload.normalizeForWire(selfieBase64);
    if (!StringUtils.hasText(idc) || !StringUtils.hasText(self)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "id card and selfie base64 are required for face match");
    }
    byte[] body = toJsonBytes(Map.of("idCardBase64", idc, "selfieBase64", self), "face match request");
    JsonNode n = postJsonNode("/api/match", body, "faceMatch");
    boolean match = n.path("match").asBoolean(false);
    double similarity = n.path("similarity").asDouble(0.0);
    if (n.has("score") && !n.has("similarity")) {
      similarity = n.path("score").asDouble(0.0);
    }
    if (n.has("similarityScore") && !n.has("similarity")) {
      similarity = n.path("similarityScore").asDouble(0.0);
    }
    return new BbsFaceMatchResult(match, similarity);
  }

  private void requireBaseUrl() {
    if (properties == null
        || properties.baseUrl() == null
        || properties.baseUrl().isBlank()) {
      log.error("cebos.bbs.base-url is not configured");
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "OCR/face service is not configured");
    }
  }

  private byte[] toJsonBytes(Object value, String logLabel) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (Exception e) {
      log.error("Failed to serialise BBS {}: {}", logLabel, e.toString());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Request encoding failed");
    }
  }

  private JsonNode postJsonNode(String path, byte[] jsonBody, String operation) {
    String base = properties.baseUrl().trim();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String url = base + path;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(Duration.ofSeconds(Math.max(1, properties.readTimeoutSeconds())))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(jsonBody))
            .build();
    try {
      if (httpLoggingProperties.isLogBbsHttp()) {
        log.info(
            "BBS outbound request operation={} url={} requestBytes={}",
            operation,
            url,
            jsonBody.length);
        if (log.isDebugEnabled()) {
          String reqLogged =
              HttpLogPayloadFormatter.formatJsonBytesForLog(
                  jsonBody, objectMapper, httpLoggingProperties);
          log.debug("BBS outbound request body operation={}\n{}", operation, reqLogged);
        }
      }
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body() == null ? "" : response.body();
      if (httpLoggingProperties.isLogBbsHttp()) {
        log.info(
            "BBS outbound response operation={} url={} status={} responseChars={}",
            operation,
            url,
            response.statusCode(),
            responseBody.length());
        if (log.isDebugEnabled()) {
          String resLogged =
              HttpLogPayloadFormatter.formatBodyForLog(
                  responseBody.getBytes(StandardCharsets.UTF_8),
                  "application/json",
                  objectMapper,
                  httpLoggingProperties);
          log.debug("BBS outbound response body operation={}\n{}", operation, resLogged);
        }
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "BBS {} failed: status={} body={}",
            operation,
            response.statusCode(),
            truncateForLog(responseBody, 200));
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "OCR/face provider returned an error");
      }
      if (response.body() == null || response.body().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OCR/face response was empty");
      }
      return objectMapper.readTree(response.body());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("BBS {} interrupted", operation);
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "OCR/face provider is unavailable");
    } catch (IOException e) {
      log.warn("BBS {} transport error: {}", operation, e.toString());
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "OCR/face provider is unavailable");
    }
  }

  private static String truncateForLog(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
