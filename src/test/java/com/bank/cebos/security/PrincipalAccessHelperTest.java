package com.bank.cebos.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PrincipalAccessHelperTest {

  @Mock private UploadBatchRepository uploadBatchRepository;

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;

  private PrincipalAccessHelper principalAccessHelper;

  @BeforeEach
  void setUp() {
    principalAccessHelper =
        new PrincipalAccessHelper(uploadBatchRepository, employeeOnboardingRepository);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void hasPortalRoleReturnsTrueForMatchingPortalPrincipalAndRole() {
    setPrincipal(PrincipalKind.PORTAL, "ROLE_ADMIN");

    assertTrue(principalAccessHelper.hasPortalRole(PortalRole.ADMIN, PortalRole.VIEWER));
  }

  @Test
  void hasPortalRoleReturnsFalseForWrongPrincipalKind() {
    setPrincipal(PrincipalKind.BANK_ADMIN, "ROLE_ADMIN");

    assertFalse(principalAccessHelper.hasPortalRole(PortalRole.ADMIN, PortalRole.VIEWER));
  }

  @Test
  void hasBankAdminRoleReturnsTrueForMatchingBankAdminRole() {
    setPrincipal(PrincipalKind.BANK_ADMIN, "ROLE_OPS_MANAGER");

    assertTrue(
        principalAccessHelper.hasBankAdminRole(
            BankAdminRole.SUPER_ADMIN,
            BankAdminRole.OPS_MANAGER,
            BankAdminRole.OPS_STAFF,
            BankAdminRole.COMPLIANCE_OFFICER,
            BankAdminRole.VIEWER));
  }

  @Test
  void hasBankAdminRoleReturnsFalseWithoutAuthentication() {
    assertFalse(principalAccessHelper.hasBankAdminRole(BankAdminRole.SUPER_ADMIN));
  }

  @Test
  void portalUserOwnsUploadBatchReturnsTrueWhenRepositoryMatches() {
    setPortalPrincipal(99L, "ROLE_ADMIN");
    when(uploadBatchRepository.existsByBatchReferenceAndCorporateClientId("BR-1", 99L))
        .thenReturn(true);

    assertTrue(principalAccessHelper.portalUserOwnsUploadBatch("BR-1"));
  }

  @Test
  void portalClientMatchesWhenPrincipalClientEqualsRequest() {
    setPortalPrincipal(42L, "ROLE_ADMIN");

    assertTrue(principalAccessHelper.portalClientMatches(42L));
    assertFalse(principalAccessHelper.portalClientMatches(99L));
  }

  @Test
  void portalClientMatchesFalseForNonPortalPrincipal() {
    setPrincipal(PrincipalKind.BANK_ADMIN, "ROLE_ADMIN");

    assertFalse(principalAccessHelper.portalClientMatches(42L));
  }

  @Test
  void portalUserOwnsUploadBatchReturnsFalseWhenCorporateClientIdMissing() {
    setPrincipal(PrincipalKind.PORTAL, "ROLE_ADMIN");

    assertFalse(principalAccessHelper.portalUserOwnsUploadBatch("BR-1"));
  }

  private static void setPortalPrincipal(Long corporateClientId, String... authorities) {
    CebosUserDetails principal =
        new CebosUserDetails(
            PrincipalKind.PORTAL,
            1L,
            corporateClientId,
            List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static void setPrincipal(PrincipalKind kind, String... authorities) {
    CebosUserDetails principal =
        new CebosUserDetails(
            kind,
            1L,
            null,
            List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
