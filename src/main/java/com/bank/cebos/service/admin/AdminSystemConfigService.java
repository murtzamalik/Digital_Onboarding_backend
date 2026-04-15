package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.AdminSystemConfigEntryResponse;
import com.bank.cebos.dto.admin.UpdateSystemConfigRequest;
import com.bank.cebos.entity.SystemConfiguration;
import com.bank.cebos.repository.SystemConfigurationRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSystemConfigService {

  private final SystemConfigurationRepository systemConfigurationRepository;

  public AdminSystemConfigService(SystemConfigurationRepository systemConfigurationRepository) {
    this.systemConfigurationRepository = systemConfigurationRepository;
  }

  @Transactional(readOnly = true)
  public Page<AdminSystemConfigEntryResponse> list(Pageable pageable) {
    return systemConfigurationRepository.findAll(pageable).map(AdminSystemConfigService::toDto);
  }

  @Transactional
  public AdminSystemConfigEntryResponse updateValue(
      long id, UpdateSystemConfigRequest request, String updatedBy) {
    SystemConfiguration row =
        systemConfigurationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Config row not found"));
    row.setConfigValue(request.configValue());
    row.setUpdatedBy(updatedBy);
    row.setUpdatedAt(Instant.now());
    return toDto(systemConfigurationRepository.save(row));
  }

  private static AdminSystemConfigEntryResponse toDto(SystemConfiguration c) {
    return new AdminSystemConfigEntryResponse(
        c.getId(),
        c.getConfigKey(),
        c.getConfigValue(),
        c.getValueType(),
        c.getDescription(),
        c.getUpdatedBy(),
        c.getUpdatedAt());
  }
}
