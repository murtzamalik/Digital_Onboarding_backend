package com.bank.cebos.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.StorageProperties;
import com.bank.cebos.dto.portal.BatchUploadResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.statemachine.OnboardingTransitionPolicy;
import com.bank.cebos.statemachine.StateMachineService;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BatchIngestServiceTest {

  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;

  @Captor private ArgumentCaptor<EmployeeOnboarding> employeeCaptor;

  private BatchIngestService batchIngestService;

  @BeforeEach
  void setUp(@TempDir java.nio.file.Path tempDir) {
    StateMachineService stateMachineService =
        new StateMachineService(
            employeeOnboardingRepository,
            employeeStatusHistoryRepository,
            new OnboardingTransitionPolicy());
    batchIngestService =
        new BatchIngestService(
            uploadBatchRepository, stateMachineService, new StorageProperties(tempDir.toString()));
  }

  @Test
  void initiateUploadParsesOneDataRowAndPersistsUploadedEmployee() throws Exception {
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
      r1.createCell(2).setCellValue("Test User");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      xlsxBytes = bos.toByteArray();
    }

    when(uploadBatchRepository.save(any(UploadBatch.class)))
        .thenAnswer(
            inv -> {
              UploadBatch b = inv.getArgument(0);
              if (b.getId() == null) {
                b.setId(500L);
              }
              return b;
            });

    when(employeeOnboardingRepository.save(any(EmployeeOnboarding.class)))
        .thenAnswer(
            inv -> {
              EmployeeOnboarding e = inv.getArgument(0);
              if (e.getId() == null) {
                e.setId(600L);
              }
              return e;
            });

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "employees.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            xlsxBytes);

    BatchUploadResponse response = batchIngestService.initiateUpload(file, 42L, 7L);

    assertThat(response.batchId()).isEqualTo(500L);
    assertThat(response.batchReference()).startsWith("BATCH-");
    assertThat(response.rowCount()).isEqualTo(1);

    verify(employeeOnboardingRepository).save(employeeCaptor.capture());
    EmployeeOnboarding saved = employeeCaptor.getValue();
    assertThat(saved.getStatus()).isEqualTo(OnboardingStatus.UPLOADED);
    assertThat(saved.getCorporateClientId()).isEqualTo(42L);
    assertThat(saved.getBatchId()).isEqualTo(500L);
    assertThat(saved.getCnic()).contains("11111");
    verify(employeeStatusHistoryRepository).save(any());
    verify(uploadBatchRepository, times(3)).save(any(UploadBatch.class));
  }
}
