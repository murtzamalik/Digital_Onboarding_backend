package com.bank.cebos.exception;

import com.bank.cebos.security.CorrelationIdFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(singleError(ex.getMessage()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put(
        "error",
        ex.getReason() != null && !ex.getReason().isBlank()
            ? ex.getReason()
            : ex.getStatusCode().toString());
    body.put("status", ex.getStatusCode().value());
    addCorrelationId(body);
    return ResponseEntity.status(ex.getStatusCode()).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> unhandled(Exception ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "Internal error");
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    addCorrelationId(body);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private static Map<String, Object> singleError(String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", message != null ? message : "Unauthorized");
    addCorrelationId(body);
    return body;
  }

  private static void addCorrelationId(Map<String, Object> body) {
    String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
    if (cid != null && !cid.isBlank()) {
      body.put("correlationId", cid);
    }
  }
}
