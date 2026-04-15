package com.bank.cebos.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.StorageProperties;
import com.bank.cebos.dto.portal.CorrectionUploadResponse;
import com.bank.cebos.entity.CorrectionBatch;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.CorrectionBatchRepository;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.Optional;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrectionIngestServiceTest {

  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private CorrectionBatchRepository correctionBatchRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  private CorrectionIngestService correctionIngestService;

  @BeforeEach
  void setUp(@TempDir java.nio.file.Path tempDir) {
    StateMachineService stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(1000L);
              }
              return e;
            });
    EmployeeOnboardingService employeeOnboardingService =
        new EmployeeOnboardingService(employeeOnboardingRepository, stateMachineService);
    UploadBatchAggregateService uploadBatchAggregateService =
        new UploadBatchAggregateService(uploadBatchRepository, employeeOnboardingRepository);
    correctionIngestService =
        new CorrectionIngestService(
            uploadBatchRepository,
            correctionBatchRepository,
            employeeOnboardingRepository,
            employeeOnboardingService,
            uploadBatchAggregateService,
            new StorageProperties(tempDir.toString()));
  }

  private static EmployeeOnboarding invalidRow() {
    try {
      var c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      EmployeeOnboarding e = c.newInstance();
      e.setId(5L);
      e.setEmployeeRef("EMP-5");
      e.setBatchId(10L);
      e.setCorporateClientId(42L);
      e.setStatus(OnboardingStatus.INVALID);
      e.setCnic("11111-1111111-1");
      e.setMobile("030");
      e.setFullName("Short mobile");
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void correctionFileRevalidatesInvalidRow() throws Exception {
    UploadBatch source = new UploadBatch();
    source.setId(10L);
    source.setBatchReference("BATCH-X");
    when(uploadBatchRepository.findByBatchReferenceAndCorporateClientId("BATCH-X", 42L))
        .thenReturn(Optional.of(source));

    when(correctionBatchRepository.save(any(CorrectionBatch.class)))
        .thenAnswer(
            inv -> {
              CorrectionBatch b = inv.getArgument(0);
              if (b.getId() == null) {
                b.setId(200L);
              }
              return b;
            });

    EmployeeOnboarding inv = invalidRow();
    when(employeeOnboardingRepository.findByBatchIdAndStatus(10L, OnboardingStatus.INVALID))
        .thenReturn(List.of(inv));

    UploadBatch batchForAggregate = new UploadBatch();
    batchForAggregate.setId(10L);
    batchForAggregate.setStatus("READY");
    when(uploadBatchRepository.findById(10L)).thenReturn(Optional.of(batchForAggregate));
    when(employeeOnboardingRepository.countByBatchId(10L)).thenReturn(1L);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(10L, OnboardingStatus.INVALID))
        .thenReturn(0L);
    when(employeeOnboardingRepository.countByBatchIdAndStatus(10L, OnboardingStatus.UPLOADED))
        .thenReturn(0L);

    byte[] xlsxBytes;
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row h = sheet.createRow(0);
      h.createCell(0).setCellValue("CNIC");
      h.createCell(1).setCellValue("MOBILE");
      h.createCell(2).setCellValue("FULL_NAME");
      Row r1 = sheet.createRow(1);
      r1.createCell(0).setCellValue("11111-1111111-1");
      r1.createCell(1).setCellValue("03001234567");
      r1.createCell(2).setCellValue("Corrected Name");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      xlsxBytes = bos.toByteArray();
    }

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "fix.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            xlsxBytes);

    CorrectionUploadResponse response =
        correctionIngestService.initiateCorrectionUpload(file, "BATCH-X", 42L, 7L);

    assertThat(response.correctionReference()).startsWith("CORR-");
    assertThat(inv.getStatus()).isEqualTo(OnboardingStatus.VALIDATED);
    assertThat(inv.getMobile()).isEqualTo("03001234567");
    assertThat(inv.getCorrectionBatchId()).isEqualTo(200L);
    verify(uploadBatchRepository).save(batchForAggregate);
    verify(employeeStatusHistoryRepository).save(any());
  }
}
