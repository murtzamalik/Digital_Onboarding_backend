package com.bank.cebos.service.onboarding;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.statemachine.StateMachineService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central read and gate layer for {@link EmployeeOnboarding}. Status mutations go through
 * {@link StateMachineService} only (including {@link StateMachineService#persistNewEmployeeOnboarding}
 * for brand-new rows).
 */
@Service
public class EmployeeOnboardingService {

  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final StateMachineService stateMachineService;

  public EmployeeOnboardingService(
      EmployeeOnboardingRepository employeeOnboardingRepository,
      StateMachineService stateMachineService) {
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.stateMachineService = stateMachineService;
  }

  public EmployeeOnboarding getRequiredById(Long id) {
    if (id == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Onboarding id required");
    }
    return employeeOnboardingRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding not found"));
  }

  public EmployeeOnboarding getRequiredByEmployeeRef(String employeeRef) {
    if (employeeRef == null || employeeRef.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employeeRef required");
    }
    return employeeOnboardingRepository
        .findByEmployeeRef(employeeRef.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding not found"));
  }

  public void requireCurrentStatus(EmployeeOnboarding employee, OnboardingStatus... allowed) {
    if (allowed.length == 0) {
      throw new IllegalArgumentException("allowed must not be empty");
    }
    requireCurrentStatus(employee, EnumSet.copyOf(Set.of(allowed)));
  }

  public void requireCurrentStatus(EmployeeOnboarding employee, Set<OnboardingStatus> allowed) {
    OnboardingStatus current = employee.getStatus();
    if (current == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Onboarding has no status");
    }
    if (!allowed.contains(current)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Action not allowed in state " + current.name());
    }
  }

  /** Same as {@link #getRequiredById} but avoids leaking existence via HTTP status for OTP flows. */
  public EmployeeOnboarding requireForMobileOtp(Long id) {
    if (id == null) {
      throw new BadCredentialsException("Invalid onboarding reference");
    }
    return employeeOnboardingRepository
        .findById(id)
        .orElseThrow(() -> new BadCredentialsException("Invalid onboarding reference"));
  }

  public void requireCurrentStatusForOtp(EmployeeOnboarding employee, Set<OnboardingStatus> allowed) {
    OnboardingStatus current = employee.getStatus();
    if (current == null || !allowed.contains(current)) {
      throw new BadCredentialsException("Invalid onboarding state for this action");
    }
  }

  /** Mobile login: same semantics as repository lookup with generic failure (no entity existence leak). */
  public EmployeeOnboarding requireByEmployeeRefForMobileLogin(String employeeRef) {
    if (employeeRef == null || employeeRef.isBlank()) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return employeeOnboardingRepository
        .findByEmployeeRef(employeeRef.trim())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
  }

  public EmployeeOnboarding requireByIdForMobileRefresh(Long id) {
    if (id == null) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    return employeeOnboardingRepository
        .findById(id)
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
  }

  public void transition(
      EmployeeOnboarding employee, OnboardingStatus toStatus, String changedBy, String reason) {
    stateMachineService.transition(employee, toStatus, changedBy, reason);
  }

  public void transition(
      EmployeeOnboarding employee,
      OnboardingStatus toStatus,
      String changedBy,
      String reason,
      boolean administrative) {
    stateMachineService.transition(employee, toStatus, changedBy, reason, administrative);
  }
}
