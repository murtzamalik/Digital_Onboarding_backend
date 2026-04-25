package com.bank.cebos.controller.admin;

import com.bank.cebos.dto.admin.CorporateClientDetailResponse;
import com.bank.cebos.dto.admin.CorporateClientListItemResponse;
import com.bank.cebos.dto.admin.CreateCorporateClientRequest;
import com.bank.cebos.entity.CorporateClient;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.service.admin.AdminCorporateClientWriteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/clients")
public class AdminCorporateClientController {

  private static final String BANK_ADMIN_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF,"
          + "T(com.bank.cebos.enums.BankAdminRole).COMPLIANCE_OFFICER,"
          + "T(com.bank.cebos.enums.BankAdminRole).VIEWER)";

  private static final String CLIENT_WRITE_SECURITY =
      "@principalAccess.hasBankAdminRole("
          + "T(com.bank.cebos.enums.BankAdminRole).SUPER_ADMIN,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_MANAGER,"
          + "T(com.bank.cebos.enums.BankAdminRole).OPS_STAFF)";

  private final CorporateClientRepository corporateClientRepository;
  private final AdminCorporateClientWriteService adminCorporateClientWriteService;

  public AdminCorporateClientController(
      CorporateClientRepository corporateClientRepository,
      AdminCorporateClientWriteService adminCorporateClientWriteService) {
    this.corporateClientRepository = corporateClientRepository;
    this.adminCorporateClientWriteService = adminCorporateClientWriteService;
  }

  @GetMapping
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<Page<CorporateClientListItemResponse>> listClients(Pageable pageable) {
    Page<CorporateClientListItemResponse> page =
        corporateClientRepository.findAll(pageable).map(this::toListItem);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{id}")
  @PreAuthorize(BANK_ADMIN_SECURITY)
  public ResponseEntity<CorporateClientDetailResponse> getClient(@PathVariable("id") long id) {
    return corporateClientRepository
        .findById(id)
        .map(this::toDetail)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize(CLIENT_WRITE_SECURITY)
  public ResponseEntity<CorporateClientDetailResponse> createClient(
      @Valid @RequestBody CreateCorporateClientRequest request) {
    return ResponseEntity.ok(adminCorporateClientWriteService.create(request));
  }

  private CorporateClientListItemResponse toListItem(CorporateClient c) {
    return new CorporateClientListItemResponse(
        c.getId(), c.getPublicId(), c.getClientCode(), c.getLegalName(), c.getStatus());
  }

  private CorporateClientDetailResponse toDetail(CorporateClient c) {
    return new CorporateClientDetailResponse(
        c.getId(),
        c.getPublicId(),
        c.getClientCode(),
        c.getLegalName(),
        c.getTradeName(),
        c.getIndustry(),
        c.getRegisteredAddress(),
        c.getCity(),
        c.getContactPhone(),
        c.getContactEmail(),
        c.getCompanyRegistrationNo(),
        c.getStatus(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
