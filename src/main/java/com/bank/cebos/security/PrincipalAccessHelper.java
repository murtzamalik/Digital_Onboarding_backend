package com.bank.cebos.security;

import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("principalAccess")
public class PrincipalAccessHelper {

  private final UploadBatchRepository uploadBatchRepository;

  public PrincipalAccessHelper(UploadBatchRepository uploadBatchRepository) {
    this.uploadBatchRepository = uploadBatchRepository;
  }

  public Optional<CebosUserDetails> getCurrentUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof CebosUserDetails userDetails)) {
      return Optional.empty();
    }
    return Optional.of(userDetails);
  }

  public boolean hasPrincipalKind(PrincipalKind expectedKind) {
    return getCurrentUserDetails().map(details -> details.kind() == expectedKind).orElse(false);
  }

  public boolean hasPortalRole(PortalRole... roles) {
    return hasRequiredRoleForKind(PrincipalKind.PORTAL, toAuthorityNames(roles));
  }

  public boolean hasBankAdminRole(BankAdminRole... roles) {
    return hasRequiredRoleForKind(PrincipalKind.BANK_ADMIN, toAuthorityNames(roles));
  }

  /** Portal principal only: JWT {@code corporateClientId} must match the request scope. */
  public boolean portalClientMatches(Long corporateClientId) {
    if (corporateClientId == null) {
      return false;
    }
    return getCurrentUserDetails()
        .filter(details -> details.kind() == PrincipalKind.PORTAL)
        .map(details -> corporateClientId.equals(details.corporateClientId()))
        .orElse(false);
  }

  /**
   * Portal-only: true when an upload batch exists for {@code batchReference} and belongs to the
   * current principal's corporate client.
   */
  public boolean portalUserOwnsUploadBatch(String batchReference) {
    if (batchReference == null || batchReference.isBlank()) {
      return false;
    }
    Optional<CebosUserDetails> detailsOpt = getCurrentUserDetails();
    if (detailsOpt.isEmpty()) {
      return false;
    }
    CebosUserDetails details = detailsOpt.get();
    if (details.kind() != PrincipalKind.PORTAL) {
      return false;
    }
    Long corporateClientId = details.corporateClientId();
    if (corporateClientId == null) {
      return false;
    }
    return uploadBatchRepository.existsByBatchReferenceAndCorporateClientId(
        batchReference, corporateClientId);
  }

  private boolean hasRequiredRoleForKind(PrincipalKind expectedKind, Set<String> requiredRoles) {
    return getCurrentUserDetails()
        .filter(details -> details.kind() == expectedKind)
        .map(CebosUserDetails::authorities)
        .stream()
        .flatMap(Collection::stream)
        .map(GrantedAuthority::getAuthority)
        .anyMatch(requiredRoles::contains);
  }

  private static Set<String> toAuthorityNames(PortalRole[] roles) {
    return Arrays.stream(roles).map(PortalRole::authority).collect(Collectors.toSet());
  }

  private static Set<String> toAuthorityNames(BankAdminRole[] roles) {
    return Arrays.stream(roles).map(BankAdminRole::authority).collect(Collectors.toSet());
  }
}
