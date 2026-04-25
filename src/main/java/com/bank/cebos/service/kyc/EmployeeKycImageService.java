package com.bank.cebos.service.kyc;

import com.bank.cebos.entity.EmployeeOnboarding;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EmployeeKycImageService {

  public Optional<byte[]> readImage(EmployeeOnboarding employee, KycImageKind kind) {
    return switch (kind) {
      case CNIC_FRONT -> firstNonEmpty(employee.getCnicFrontImageData(), employee.getCnicFrontImagePath());
      case CNIC_BACK -> firstNonEmpty(employee.getCnicBackImageData(), employee.getCnicBackImagePath());
      case SELFIE -> firstNonEmpty(employee.getSelfieImageData(), employee.getSelfieImagePath());
    };
  }

  private static Optional<byte[]> firstNonEmpty(byte[] data, String path) {
    if (data != null && data.length > 0) {
      return Optional.of(data);
    }
    if (path == null || path.isBlank()) {
      return Optional.empty();
    }
    try {
      Path p = Path.of(path.trim());
      if (!Files.isRegularFile(p)) {
        return Optional.empty();
      }
      return Optional.of(Files.readAllBytes(p));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
