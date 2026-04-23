package com.bank.cebos.service.batch;

import com.bank.cebos.config.StorageProperties;
import com.bank.cebos.dto.portal.BatchUploadResponse;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.statemachine.StateMachineService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchIngestService {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();

  private final UploadBatchRepository uploadBatchRepository;
  private final StateMachineService stateMachineService;
  private final StorageProperties storageProperties;

  public BatchIngestService(
      UploadBatchRepository uploadBatchRepository,
      StateMachineService stateMachineService,
      StorageProperties storageProperties) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.stateMachineService = stateMachineService;
    this.storageProperties = storageProperties;
  }

  @Transactional
  public BatchUploadResponse initiateUpload(
      MultipartFile file, Long corporateClientId, Long uploadedByUserId) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx files are supported");
    }

    String batchRef =
        "BATCH-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

    UploadBatch batch = new UploadBatch();
    batch.setCorporateClientId(corporateClientId);
    batch.setUploadedByUserId(uploadedByUserId);
    batch.setBatchReference(batchRef);
    batch.setOriginalFilename(originalName);
    batch.setStatus("PROCESSING");
    batch.setTotalRows(0);
    batch.setValidRowCount(0);
    batch.setInvalidRowCount(0);
    batch = uploadBatchRepository.save(batch);

    Path dest;
    try {
      Path dir =
          Path.of(storageProperties.uploadDir())
              .resolve("client-" + corporateClientId)
              .resolve(batchRef);
      Files.createDirectories(dir);
      String safeName = sanitizeFilename(originalName);
      dest = dir.resolve(safeName);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store upload", e);
    }

    batch.setStoragePath(dest.toString());
    uploadBatchRepository.save(batch);

    int rowCount;
    try {
      rowCount = parseWorkbookAndPersistEmployees(dest, batch, corporateClientId, uploadedByUserId);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Excel workbook", e);
    }

    batch.setTotalRows(rowCount);
    uploadBatchRepository.save(batch);

    return new BatchUploadResponse(batch.getId(), batchRef, rowCount);
  }

  private int parseWorkbookAndPersistEmployees(
      Path xlsxPath,
      UploadBatch batch,
      Long corporateClientId,
      Long uploadedByUserId)
      throws IOException {
    int inserted = 0;
    try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(xlsxPath))) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        return 0;
      }
      Row header = sheet.getRow(0);
      if (header == null) {
        return 0;
      }
      Map<String, Integer> col = headerIndex(header);
      Integer ixCnic = col.get("CNIC");
      Integer ixMobile = col.get("MOBILE");
      Integer ixName = col.get("FULL_NAME");
      Integer ixMotherName = col.get("MOTHER_NAME");
      if (ixCnic == null || ixMobile == null || ixName == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Excel header row must include CNIC, MOBILE, FULL_NAME (MOTHER_NAME is optional)");
      }

      int last = sheet.getLastRowNum();
      for (int r = 1; r <= last; r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        String cnic = cellString(row, ixCnic).trim();
        String mobile = cellString(row, ixMobile).trim();
        String fullName = cellString(row, ixName).trim();
        if (cnic.isEmpty() && mobile.isEmpty() && fullName.isEmpty()) {
          continue;
        }
        if (cnic.isEmpty() || mobile.isEmpty() || fullName.isEmpty()) {
          continue;
        }

        EmployeeOnboarding employee = newEmployeeOnboarding();
        employee.setEmployeeRef(
            "EMP-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        employee.setBatchId(batch.getId());
        employee.setCorporateClientId(corporateClientId);
        employee.setStatus(OnboardingStatus.UPLOADED);
        employee.setCnic(cnic);
        employee.setMobile(mobile);
        employee.setFullName(fullName);
        if (ixMotherName != null) {
          String motherName = cellString(row, ixMotherName).trim();
          if (!motherName.isEmpty()) {
            employee.setMotherName(motherName);
          }
        }

        stateMachineService.persistNewEmployeeOnboarding(
            employee,
            "portal-user:" + uploadedByUserId,
            "Excel bulk upload row for batch " + batch.getBatchReference());
        inserted++;
      }
    }
    return inserted;
  }

  private static Map<String, Integer> headerIndex(Row header) {
    Map<String, Integer> m = new HashMap<>();
    short last = header.getLastCellNum();
    for (int c = 0; c < last; c++) {
      Cell cell = header.getCell(c);
      if (cell == null) {
        continue;
      }
      String key = DATA_FORMATTER.formatCellValue(cell).trim().toUpperCase(Locale.ROOT);
      if (!key.isEmpty()) {
        m.put(key, c);
      }
    }
    return m;
  }

  private static String cellString(Row row, int colIndex) {
    Cell cell = row.getCell(colIndex);
    if (cell == null) {
      return "";
    }
    return DATA_FORMATTER.formatCellValue(cell);
  }

  private static EmployeeOnboarding newEmployeeOnboarding() {
    try {
      var c = EmployeeOnboarding.class.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String sanitizeFilename(String name) {
    String base = name.replace("\\", "_").replace("/", "_");
    if (base.length() > 200) {
      base = base.substring(base.length() - 200);
    }
    return base.isEmpty() ? "upload.xlsx" : base;
  }
}
