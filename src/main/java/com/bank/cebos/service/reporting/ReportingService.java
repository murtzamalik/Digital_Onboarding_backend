package com.bank.cebos.service.reporting;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportingService {

  private static final int BLOCKED_PAGE_SIZE = 500;

  private final UploadBatchRepository uploadBatchRepository;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;

  public ReportingService(
      UploadBatchRepository uploadBatchRepository,
      EmployeeOnboardingRepository employeeOnboardingRepository) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
  }

  public void streamBatchReport(String batchRef, long corporateClientId, OutputStream out)
      throws IOException {
    UploadBatch batch =
        uploadBatchRepository
            .findByBatchReferenceAndCorporateClientId(batchRef, corporateClientId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Batch not found for this client"));
    try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      w.write("batch_reference,status,total_rows,valid_rows,invalid_rows\n");
      w.write(
          csvField(batch.getBatchReference())
              + ","
              + csvField(batch.getStatus())
              + ","
              + batch.getTotalRows()
              + ","
              + batch.getValidRowCount()
              + ","
              + batch.getInvalidRowCount()
              + "\n");
      w.write("employee_ref,status,cnic_masked,mobile_masked,full_name\n");
      for (EmployeeOnboarding e : employeeOnboardingRepository.findByBatchId(batch.getId())) {
        w.write(
            csvField(e.getEmployeeRef())
                + ","
                + csvField(e.getStatus() != null ? e.getStatus().name() : "")
                + ","
                + csvField(maskTail(e.getCnic(), 4))
                + ","
                + csvField(maskTail(e.getMobile(), 4))
                + ","
                + csvField(e.getFullName())
                + "\n");
      }
      w.flush();
    }
  }

  public void streamMonthlyReport(String yearMonth, long corporateClientId, OutputStream out)
      throws IOException {
    YearMonth ym;
    try {
      ym = YearMonth.parse(yearMonth);
    } catch (DateTimeException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "month must be in format yyyy-MM");
    }
    Instant start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    long batchCount =
        uploadBatchRepository.countByCorporateClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            corporateClientId, start, end);
    long totalRows =
        uploadBatchRepository.sumTotalRowsByCorporateClientIdAndCreatedAtRange(
            corporateClientId, start, end);
    try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      w.write("month,batch_count,total_rows_uploaded\n");
      w.write(yearMonth + "," + batchCount + "," + totalRows + "\n");
      w.flush();
    }
  }

  public void streamBlockedReport(long corporateClientId, OutputStream out) throws IOException {
    try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      w.write("employee_ref,block_reason,blocked_at\n");
      for (int page = 0; ; page++) {
        Page<EmployeeOnboarding> slice =
            employeeOnboardingRepository.findByCorporateClientIdAndStatus(
                corporateClientId,
                OnboardingStatus.BLOCKED,
                PageRequest.of(page, BLOCKED_PAGE_SIZE));
        if (slice.isEmpty()) {
          break;
        }
        for (EmployeeOnboarding e : slice.getContent()) {
          w.write(
              csvField(e.getEmployeeRef())
                  + ","
                  + csvField(e.getBlockReason())
                  + ","
                  + (e.getBlockedAt() != null ? e.getBlockedAt().toString() : "")
                  + "\n");
        }
        if (!slice.hasNext()) {
          break;
        }
      }
      w.flush();
    }
  }

  static String maskTail(String value, int visible) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String t = value.trim();
    if (t.length() <= visible) {
      return "****";
    }
    return "*".repeat(t.length() - visible) + t.substring(t.length() - visible);
  }

  static String csvField(String s) {
    if (s == null) {
      return "";
    }
    String t = s.replace("\"", "\"\"");
    if (t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
      return "\"" + t + "\"";
    }
    return t;
  }
}
