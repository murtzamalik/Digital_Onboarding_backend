package com.bank.cebos.statemachine;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.entity.EmployeeStatusHistory;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.EmployeeStatusHistoryRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central authority for persisting changes to {@link EmployeeOnboarding#getStatus()}. Application
 * code must not assign onboarding status outside this service; the transition matrix, history
 * writes, and side effects will be implemented in later iterations.
 */
@Service
public class StateMachineService {

  private static final Logger log = LoggerFactory.getLogger(StateMachineService.class);

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
  private final OnboardingTransitionPolicy onboardingTransitionPolicy;

  public StateMachineService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      EmployeeStatusHistoryRepository employeeStatusHistoryRepository,
      OnboardingTransitionPolicy onboardingTransitionPolicy) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.employeeStatusHistoryRepository = employeeStatusHistoryRepository;
    this.onboardingTransitionPolicy = onboardingTransitionPolicy;
  }

  /**
   * Transitions an onboarding aggregate to {@code toStatus}. This is the only supported mutator
   * for {@link EmployeeOnboarding#setStatus(OnboardingStatus)} in the codebase.
   */
  @Transactional
  public void transition(
      EmployeeOnboarding employee,
      OnboardingStatus toStatus,
      String changedBy,
      String reason) {
    transition(employee, toStatus, changedBy, reason, false, null, null);
  }

  @Transactional
  public void transition(
      EmployeeOnboarding employee,
      OnboardingStatus toStatus,
      String changedBy,
      String reason,
      boolean administrative) {
    transition(employee, toStatus, changedBy, reason, administrative, null, null);
  }

  @Transactional
  public void transition(
      EmployeeOnboarding employee,
      OnboardingStatus toStatus,
      String changedBy,
      String reason,
      boolean administrative,
      String ipAddress,
      String correlationId) {
    Objects.requireNonNull(employee, "employee");
    Objects.requireNonNull(toStatus, "toStatus");
    Objects.requireNonNull(changedBy, "changedBy");

    OnboardingStatus from = employee.getStatus();
    if (from == null) {
      throw new IllegalStateException("Current status must be set before transition");
    }
    if (from == toStatus) {
      return;
    }

    onboardingTransitionPolicy.validate(from, toStatus, administrative);

    log.info(
        "Onboarding transition employeeRef={} from={} to={} changedBy={} reason={} administrative={}",
        employee.getEmployeeRef(),
        from,
        toStatus,
        changedBy,
        reason,
        administrative);

    employee.setStatus(toStatus);
    employeeOnboardingRepository.save(employee);

    EmployeeStatusHistory history = new EmployeeStatusHistory();
    history.setEmployeeOnboarding(employee);
    history.setFromStatus(from.name());
    history.setToStatus(toStatus.name());
    history.setChangedBy(changedBy);
    history.setReason(reason);
    history.setIpAddress(ipAddress);
    history.setCorrelationId(correlationId);
    employeeStatusHistoryRepository.save(history);
  }

  /**
   * Persists a brand-new onboarding row at {@link OnboardingStatus#UPLOADED} with initial history
   * (no prior state). Use {@link #transition} for all subsequent status changes on persisted rows.
   */
  @Transactional
  public EmployeeOnboarding persistNewEmployeeOnboarding(
      EmployeeOnboarding employee, String changedBy, String reason) {
    Objects.requireNonNull(employee, "employee");
    Objects.requireNonNull(changedBy, "changedBy");
    if (employee.getId() != null) {
      throw new IllegalArgumentException("persistNewEmployeeOnboarding requires a new entity (id null)");
    }
    if (employee.getStatus() != OnboardingStatus.UPLOADED) {
      throw new IllegalArgumentException(
          "persistNewEmployeeOnboarding requires status UPLOADED, got " + employee.getStatus());
    }
    Objects.requireNonNull(employee.getEmployeeRef(), "employeeRef");
    Objects.requireNonNull(employee.getBatchId(), "batchId");
    Objects.requireNonNull(employee.getCorporateClientId(), "corporateClientId");

    log.info(
        "Onboarding initial persist employeeRef={} status={} changedBy={} reason={}",
        employee.getEmployeeRef(),
        employee.getStatus(),
        changedBy,
        reason);

    employeeOnboardingRepository.save(employee);

    EmployeeStatusHistory history = new EmployeeStatusHistory();
    history.setEmployeeOnboarding(employee);
    history.setFromStatus(null);
    history.setToStatus(OnboardingStatus.UPLOADED.name());
    history.setChangedBy(changedBy);
    history.setReason(reason);
    employeeStatusHistoryRepository.save(history);
    return employee;
  }
}
