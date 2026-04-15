package com.bank.cebos.service.portal;

import com.bank.cebos.dto.portal.PortalCorporateUserResponse;
import com.bank.cebos.dto.portal.PortalCreateCorporateUserRequest;
import com.bank.cebos.entity.CorporateUser;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.repository.CorporateUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortalCorporateUserService {

  private static final String ACTIVE = "ACTIVE";

  private final CorporateUserRepository corporateUserRepository;
  private final PasswordEncoder passwordEncoder;

  public PortalCorporateUserService(
      CorporateUserRepository corporateUserRepository, PasswordEncoder passwordEncoder) {
    this.corporateUserRepository = corporateUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public Page<PortalCorporateUserResponse> listForClient(long corporateClientId, Pageable pageable) {
    return corporateUserRepository
        .findByCorporateClientId(corporateClientId, pageable)
        .map(PortalCorporateUserService::toResponse);
  }

  @Transactional
  public PortalCorporateUserResponse createForClient(
      long corporateClientId, PortalCreateCorporateUserRequest request) {
    String normalizedEmail = request.email().trim();
    if (corporateUserRepository.existsByCorporateClientIdAndEmailIgnoreCase(
        corporateClientId, normalizedEmail)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Email already registered for this organization");
    }
    PortalRole role;
    try {
      role = PortalRole.valueOf(request.role().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be ADMIN or VIEWER");
    }
    CorporateUser user = new CorporateUser();
    user.setCorporateClientId(corporateClientId);
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFullName(request.fullName().trim());
    user.setRole(role.name());
    user.setStatus(ACTIVE);
    CorporateUser saved = corporateUserRepository.save(user);
    return toResponse(saved);
  }

  private static PortalCorporateUserResponse toResponse(CorporateUser u) {
    return new PortalCorporateUserResponse(
        u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStatus());
  }
}
