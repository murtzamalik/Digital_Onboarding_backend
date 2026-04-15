package com.bank.cebos.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.statemachine.StateMachineService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAmlComplianceServiceTest {

  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private StateMachineService stateMachineService;

  private AdminAmlComplianceService service;

  @BeforeEach
  void setUp() {
    service = new AdminAmlComplianceService(employeeOnboardingRepository, stateMachineService);
  }

  @Test
  void clearRejectedRunsTwoTransitions() {
    EmployeeOnboarding e = new EmployeeOnboarding();
    e.setEmployeeRef("E-1");
    e.setStatus(OnboardingStatus.AML_REJECTED);
    when(employeeOnboardingRepository.findByEmployeeRef("E-1")).thenReturn(Optional.of(e));

    service.clearRejectedToT24("E-1", 42L, "cid-1");

    InOrder inOrder = inOrder(stateMachineService);
    inOrder
        .verify(stateMachineService)
        .transition(
            eq(e),
            eq(OnboardingStatus.AML_CHECK_PENDING),
            eq("bank-admin:42"),
            any(),
            eq(true),
            eq(null),
            eq("cid-1"));
    inOrder
        .verify(stateMachineService)
        .transition(
            eq(e),
            eq(OnboardingStatus.T24_PENDING),
            eq("bank-admin:42"),
            any(),
            eq(false),
            eq(null),
            eq("cid-1"));
  }
}
