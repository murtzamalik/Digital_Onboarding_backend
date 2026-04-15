package com.bank.cebos.service.audit;

import com.bank.cebos.entity.PortalUserAuditLog;
import com.bank.cebos.repository.PortalUserAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PortalUserAuditService {

  public static final String ACTION_PASSWORD_RESET_REQUESTED = "PORTAL_PASSWORD_RESET_REQUESTED";
  public static final String ACTION_PASSWORD_RESET_COMPLETED = "PORTAL_PASSWORD_RESET_COMPLETED";

  private final PortalUserAuditLogRepository portalUserAuditLogRepository;
  private final ObjectMapper objectMapper;

  public PortalUserAuditService(
      PortalUserAuditLogRepository portalUserAuditLogRepository, ObjectMapper objectMapper) {
    this.portalUserAuditLogRepository = portalUserAuditLogRepository;
    this.objectMapper = objectMapper;
  }

  public void recordPasswordResetRequested(
      long corporateUserId, String ipAddress, String correlationId) {
    record(
        corporateUserId,
        ACTION_PASSWORD_RESET_REQUESTED,
        "PASSWORD_RESET",
        null,
        jsonDetail("request"),
        ipAddress,
        correlationId);
  }

  public void recordPasswordResetCompleted(long corporateUserId) {
    record(
        corporateUserId,
        ACTION_PASSWORD_RESET_COMPLETED,
        "PASSWORD_RESET",
        null,
        jsonDetail("complete"),
        null,
        null);
  }

  private void record(
      Long corporateUserId,
      String action,
      String resourceType,
      String resourceId,
      String detailJson,
      String ipAddress,
      String correlationId) {
    portalUserAuditLogRepository.save(
        new PortalUserAuditLog(
            corporateUserId,
            action,
            resourceType,
            resourceId,
            detailJson,
            truncateIp(ipAddress),
            correlationId));
  }

  private String jsonDetail(String phase) {
    try {
      return objectMapper.writeValueAsString(Map.of("phase", phase));
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  private static String truncateIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return null;
    }
    return ip.length() > 64 ? ip.substring(0, 64) : ip;
  }
}
