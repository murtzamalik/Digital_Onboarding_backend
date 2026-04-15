package com.bank.cebos.service.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;

  @Test
  void maskTailShowsLastDigitsOnly() {
    assertThat(ReportingService.maskTail("11111-1111111-1", 4)).endsWith("11-1");
    assertThat(ReportingService.maskTail("0300", 4)).isEqualTo("****");
  }

  @Test
  void streamBatchReportWritesBatchAndEmployeeLines() throws Exception {
    UploadBatch batch = new UploadBatch();
    batch.setId(10L);
    batch.setBatchReference("BATCH-A");
    batch.setStatus("READY");
    batch.setTotalRows(1);
    batch.setValidRowCount(1);
    batch.setInvalidRowCount(0);
    when(uploadBatchRepository.findByBatchReferenceAndCorporateClientId("BATCH-A", 7L))
        .thenReturn(Optional.of(batch));

    EmployeeOnboarding row = newEmployee("EMP-1", OnboardingStatus.VALIDATED, "11111-1111111-1");
    when(employeeOnboardingRepository.findByBatchId(10L)).thenReturn(List.of(row));

    ReportingService service = new ReportingService(uploadBatchRepository, employeeOnboardingRepository);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.streamBatchReport("BATCH-A", 7L, out);

    String csv = out.toString();
    assertThat(csv).contains("BATCH-A");
    assertThat(csv).contains("EMP-1");
    assertThat(csv).contains("VALIDATED");
  }

  @Test
  void streamMonthlyReportUsesRepositoryAggregates() throws Exception {
    when(uploadBatchRepository.countByCorporateClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            eq(5L), any(Instant.class), any(Instant.class)))
        .thenReturn(3L);
    when(uploadBatchRepository.sumTotalRowsByCorporateClientIdAndCreatedAtRange(
            eq(5L), any(Instant.class), any(Instant.class)))
        .thenReturn(99L);

    ReportingService service = new ReportingService(uploadBatchRepository, employeeOnboardingRepository);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.streamMonthlyReport("2026-03", 5L, out);

    assertThat(out.toString()).contains("2026-03").contains("3").contains("99");
  }

  @Test
  void streamBlockedReportPagesThroughBlockedEmployees() throws Exception {
    EmployeeOnboarding blocked = newEmployee("E-B", OnboardingStatus.BLOCKED, null);
    blocked.setBlockReason("AML");
    blocked.setBlockedAt(Instant.parse("2026-04-01T00:00:00Z"));
    when(employeeOnboardingRepository.findByCorporateClientIdAndStatus(
            eq(8L), eq(OnboardingStatus.BLOCKED), eq(PageRequest.of(0, 500))))
        .thenReturn(new PageImpl<>(List.of(blocked)));

    ReportingService service = new ReportingService(uploadBatchRepository, employeeOnboardingRepository);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.streamBlockedReport(8L, out);

    assertThat(out.toString()).contains("E-B").contains("AML");
  }

  private static EmployeeOnboarding newEmployee(String ref, OnboardingStatus status, String cnic) {
    try {
      Constructor<EmployeeOnboarding> c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setEmployeeRef(ref);
      e.setStatus(status);
      e.setCnic(cnic);
      e.setMobile("03001234567");
      e.setFullName("Test");
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
