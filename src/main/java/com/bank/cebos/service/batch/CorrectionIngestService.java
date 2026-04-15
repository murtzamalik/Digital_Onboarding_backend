package com.bank.cebos.service.batch;

import com.bank.cebos.config.StorageProperties;
import com.bank.cebos.dto.portal.CorrectionUploadResponse;
import com.bank.cebos.entity.CorrectionBatch;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.UploadBatch;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.CorrectionBatchRepository;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Correction uploads: match rows to {@link OnboardingStatus#INVALID} employees in the source batch by
 * CNIC, apply corrected fields, re-validate, then move to {@link OnboardingStatus#VALIDATED} only via
 * {@link EmployeeOnboardingService}.
 */
@Service
public class CorrectionIngestService {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();
  private static final String STATUS_COMPLETED = "COMPLETED";

  private final UploadBatchRepository uploadBatchRepository;
  private final CorrectionBatchRepository correctionBatchRepository;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final EmployeeOnboardingService employeeOnboardingService;
  private final UploadBatchAggregateService uploadBatchAggregateService;
  private final StorageProperties storageProperties;

  public CorrectionIngestService(
      UploadBatchRepository uploadBatchRepository,
      CorrectionBatchRepository correctionBatchRepository,
      EmployeeOnboardingRepository employeeOnboardingRepository,
      EmployeeOnboardingService employeeOnboardingService,
      UploadBatchAggregateService uploadBatchAggregateService,
      StorageProperties storageProperties) {
    this.uploadBatchRepository = uploadBatchRepository;
    this.correctionBatchRepository = correctionBatchRepository;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.employeeOnboardingService = employeeOnboardingService;
    this.uploadBatchAggregateService = uploadBatchAggregateService;
    this.storageProperties = storageProperties;
  }

  @Transactional
  public CorrectionUploadResponse initiateCorrectionUpload(
      MultipartFile file, String sourceBatchRef, Long corporateClientId, Long uploadedByUserId) {
    if (uploadedByUserId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploader context required");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx files are supported");
    }

    UploadBatch source =
        uploadBatchRepository
            .findByBatchReferenceAndCorporateClientId(sourceBatchRef, corporateClientId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Batch not found for this client"));

    String correctionRef =
        "CORR-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

    CorrectionBatch correction = new CorrectionBatch();
    correction.setCorporateClientId(corporateClientId);
    correction.setSourceBatchId(source.getId());
    correction.setCorrectionReference(correctionRef);
    correction.setOriginalFilename(originalName);
    correction.setStatus("PROCESSING");
    correction = correctionBatchRepository.save(correction);

    Path dest;
    try {
      Path dir =
          Path.of(storageProperties.uploadDir())
              .resolve("client-" + corporateClientId)
              .resolve("corrections")
              .resolve(correctionRef);
      Files.createDirectories(dir);
      String safeName = originalName.replace("\\", "_").replace("/", "_");
      dest = dir.resolve(safeName.isEmpty() ? "correction.xlsx" : safeName);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
      }
      correction.setStoragePath(dest.toString());
      correctionBatchRepository.save(correction);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store correction upload", e);
    }

    Map<String, EmployeeOnboarding> invalidByCnic = new HashMap<>();
    for (EmployeeOnboarding row :
        employeeOnboardingRepository.findByBatchIdAndStatus(
            source.getId(), OnboardingStatus.INVALID)) {
      String key = BulkUploadRowRules.normalizeCnicKey(row.getCnic());
      if (!key.isEmpty()) {
        invalidByCnic.putIfAbsent(key, row);
      }
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(dest))) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheet");
      }
      Row header = sheet.getRow(0);
      if (header == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel header row is required");
      }
      Map<String, Integer> col = headerIndex(header);
      Integer ixCnic = col.get("CNIC");
      Integer ixMobile = col.get("MOBILE");
      Integer ixName = col.get("FULL_NAME");
      if (ixCnic == null || ixMobile == null || ixName == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Excel header row must include CNIC, MOBILE, FULL_NAME");
      }

      Set<String> seenCnicKeys = new HashSet<>();
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
        String key = BulkUploadRowRules.normalizeCnicKey(cnic);
        if (key.isEmpty()) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "CNIC is required for correction row " + (r + 1));
        }
        if (!seenCnicKeys.add(key)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Duplicate CNIC in correction file: " + cnic);
        }
        EmployeeOnboarding employee = invalidByCnic.get(key);
        if (employee == null) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "CNIC does not match an INVALID employee in this batch: " + cnic);
        }
        employee.setCnic(cnic);
        employee.setMobile(mobile);
        employee.setFullName(fullName);
        List<String> errors =
            BulkUploadRowRules.validate(
                employee.getCnic(), employee.getMobile(), employee.getFullName());
        if (!errors.isEmpty()) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }
        employee.setValidationErrors(null);
        employee.setCorrectionBatchId(correction.getId());
        employeeOnboardingService.transition(
            employee,
            OnboardingStatus.VALIDATED,
            "portal-correction:" + uploadedByUserId,
            "Corrected via " + correctionRef);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid Excel workbook", e);
    }

    correction.setStatus(STATUS_COMPLETED);
    correctionBatchRepository.save(correction);
    uploadBatchAggregateService.refreshBatch(source.getId());

    return new CorrectionUploadResponse(correction.getId(), correctionRef);
  }

  private static Map<String, Integer> headerIndex(Row header) {
    Map<String, Integer> m = new HashMap<>();
    short lastCell = header.getLastCellNum();
    for (int c = 0; c < lastCell; c++) {
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
}
