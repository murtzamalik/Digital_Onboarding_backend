package com.bank.cebos.logging;

import com.bank.cebos.config.HttpLoggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Logs inbound HTTP method, path, selected headers, request body, response status, and response
 * body. Authorization and cookie values are redacted. Skips paths under {@link
 * HttpLoggingProperties#getExcludedPathPrefixes()}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpExchangeLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(HttpExchangeLoggingFilter.class);

  private final HttpLoggingProperties props;
  private final ObjectMapper objectMapper;

  public HttpExchangeLoggingFilter(HttpLoggingProperties props, ObjectMapper objectMapper) {
    this.props = props;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!props.isEnabled()) {
      return true;
    }
    String uri = request.getRequestURI();
    for (String prefix : props.getExcludedPathPrefixes()) {
      if (prefix != null && !prefix.isEmpty() && uri.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    int cacheLimit = Math.max(1024, props.getMaxCachedBodyBytes());
    ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, cacheLimit);
    // Spring 6.1 ContentCachingResponseWrapper has no cache limit setter; large bodies still buffer
    // for typical JSON API sizes. Request side uses explicit cacheLimit above.
    ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
    long startNs = System.nanoTime();
    try {
      filterChain.doFilter(req, res);
    } finally {
      try {
        long ms = (System.nanoTime() - startNs) / 1_000_000L;
        logExchange(req, res, ms);
      } finally {
        res.copyBodyToResponse();
      }
    }
  }

  private void logExchange(
      ContentCachingRequestWrapper req, ContentCachingResponseWrapper res, long durationMs) {
    String method = req.getMethod();
    String uri = req.getRequestURI();
    String query = req.getQueryString();
    String pathWithQuery = query == null ? uri : uri + "?" + query;
    int status = res.getStatus();
    Map<String, List<String>> headers = safeHeaders(req);
    String reqContentType = req.getContentType();
    byte[] reqBytes = req.getContentAsByteArray();
    String reqBody =
        HttpLogPayloadFormatter.formatBodyForLog(
            reqBytes, reqContentType, objectMapper, props);
    String resContentType = res.getContentType();
    byte[] resBytes = res.getContentAsByteArray();
    String resBody =
        HttpLogPayloadFormatter.formatBodyForLog(
            resBytes, resContentType, objectMapper, props);

    log.info("HTTP inbound {} {} -> {} ({} ms)", method, pathWithQuery, status, durationMs);
    if (log.isDebugEnabled()) {
      log.debug(
          "HTTP inbound detail {} {}\nrequestHeaders={}\nrequestBody=\n{}\nresponseBody=\n{}",
          method,
          pathWithQuery,
          headers,
          reqBody,
          resBody);
    }
  }

  private static Map<String, List<String>> safeHeaders(HttpServletRequest req) {
    Map<String, List<String>> out = new LinkedHashMap<>();
    Collections.list(req.getHeaderNames())
        .forEach(
            name -> {
              if (name == null) {
                return;
              }
              List<String> values = Collections.list(req.getHeaders(name));
              if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                  || HttpHeaders.COOKIE.equalsIgnoreCase(name)
                  || "set-cookie".equalsIgnoreCase(name)) {
                out.put(name, List.of("REDACTED"));
              } else {
                out.put(name, values.stream().map(String::valueOf).collect(Collectors.toList()));
              }
            });
    return out;
  }
}
