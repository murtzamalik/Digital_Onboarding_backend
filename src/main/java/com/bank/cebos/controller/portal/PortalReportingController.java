package com.bank.cebos.controller.portal;

import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.reporting.ReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/portal/reports")
public class PortalReportingController {

  private static final String PORTAL_ROLE_AND_KIND_SECURITY =
      "hasAnyRole('ADMIN','VIEWER') and @principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).PORTAL)";

  private static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv");

  private final ReportingService reportingService;

  public PortalReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @GetMapping("/batch/{batchRef}")
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<StreamingResponseBody> streamBatchReport(
      @PathVariable String batchRef, @AuthenticationPrincipal CebosUserDetails principal) {
    long corporateClientId = requirePortalCorporateClientId(principal);
    StreamingResponseBody body =
        out -> reportingService.streamBatchReport(batchRef, corporateClientId, out);
    return ResponseEntity.ok()
        .contentType(CSV_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"batch-report-" + batchRef + ".csv\"")
        .body(body);
  }

  @GetMapping("/monthly")
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<StreamingResponseBody> streamMonthlyReport(
      @RequestParam("month") String yearMonth, @AuthenticationPrincipal CebosUserDetails principal) {
    long corporateClientId = requirePortalCorporateClientId(principal);
    StreamingResponseBody body =
        out -> reportingService.streamMonthlyReport(yearMonth, corporateClientId, out);
    return ResponseEntity.ok()
        .contentType(CSV_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"monthly-report-" + yearMonth + ".csv\"")
        .body(body);
  }

  @GetMapping("/blocked")
  @PreAuthorize(PORTAL_ROLE_AND_KIND_SECURITY)
  public ResponseEntity<StreamingResponseBody> streamBlockedReport(
      @AuthenticationPrincipal CebosUserDetails principal) {
    long corporateClientId = requirePortalCorporateClientId(principal);
    StreamingResponseBody body =
        out -> reportingService.streamBlockedReport(corporateClientId, out);
    return ResponseEntity.ok()
        .contentType(CSV_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"blocked-report-" + corporateClientId + ".csv\"")
        .body(body);
  }

  private static long requirePortalCorporateClientId(CebosUserDetails principal) {
    if (principal == null || principal.corporateClientId() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Portal client scope required for reports");
    }
    return principal.corporateClientId();
  }
}
