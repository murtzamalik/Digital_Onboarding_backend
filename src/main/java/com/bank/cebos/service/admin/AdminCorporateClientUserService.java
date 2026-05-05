package com.bank.cebos.service.admin;

import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.service.portal.PortalCorporateUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Bank-admin API: list / create portal users for a given corporate client. */
@Service
public class AdminCorporateClientUserService {

  private final CorporateClientRepository corporateClientRepository;
  private final PortalCorporateUserService portalCorporateUserService;

  public AdminCorporateClientUserService(
      CorporateClientRepository corporateClientRepository,
      PortalCorporateUserService portalCorporateUserService) {
    this.corporateClientRepository = corporateClientRepository;
    this.portalCorporateUserService = portalCorporateUserService;
  }

  @Transactional(readOnly = true)
  public Page<PortalCorporateUserResponse> listUsers(long corporateClientId, Pageable pageable) {
    requireClient(corporateClientId);
    return portalCorporateUserService.listForClient(corporateClientId, pageable);
  }

  @Transactional
  public PortalCorporateUserResponse createUser(
      long corporateClientId, PortalCreateCorporateUserRequest request) {
    requireClient(corporateClientId);
    return portalCorporateUserService.createForClient(corporateClientId, request);
  }

  private void requireClient(long corporateClientId) {
    corporateClientRepository
        .findById(corporateClientId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
  }
}
