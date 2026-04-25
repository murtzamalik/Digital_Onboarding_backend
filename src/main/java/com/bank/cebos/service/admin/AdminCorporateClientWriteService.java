package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.CorporateClientDetailResponse;
import com.bank.cebos.dto.admin.CreateCorporateClientRequest;
import com.bank.cebos.entity.CorporateClient;
import com.bank.cebos.repository.CorporateClientRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCorporateClientWriteService {

  private static final String ACTIVE = "ACTIVE";

  private final CorporateClientRepository corporateClientRepository;

  public AdminCorporateClientWriteService(CorporateClientRepository corporateClientRepository) {
    this.corporateClientRepository = corporateClientRepository;
  }

  @Transactional
  public CorporateClientDetailResponse create(CreateCorporateClientRequest request) {
    String code = request.clientCode().trim();
    if (corporateClientRepository.existsByClientCodeIgnoreCase(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Client code already exists");
    }
    Instant now = Instant.now();
    CorporateClient c = new CorporateClient();
    c.setPublicId(UUID.randomUUID().toString());
    c.setClientCode(code);
    c.setLegalName(request.legalName().trim());
    c.setTradeName(request.tradeName());
    c.setIndustry(request.industry());
    c.setRegisteredAddress(request.registeredAddress());
    c.setCity(request.city());
    c.setContactPhone(request.contactPhone());
    c.setContactEmail(request.contactEmail());
    c.setCompanyRegistrationNo(request.companyRegistrationNo());
    c.setStatus(ACTIVE);
    c.setCreatedAt(now);
    c.setUpdatedAt(now);
    CorporateClient saved = corporateClientRepository.save(c);
    return toDetail(saved);
  }

  private static CorporateClientDetailResponse toDetail(CorporateClient c) {
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
