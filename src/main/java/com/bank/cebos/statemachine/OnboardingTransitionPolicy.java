package com.bank.cebos.statemachine;

import com.bank.cebos.enums.OnboardingStatus;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Allowed onboarding status transitions for CEBOS v2. Automatic transitions follow the mobile
 * journey; {@code administrative} unlocks {@link OnboardingStatus#BLOCKED} recovery edges only.
 */
@Component
public class OnboardingTransitionPolicy {

  private static final Set<OnboardingStatus> TERMINAL =
      EnumSet.of(OnboardingStatus.ACCOUNT_OPENED, OnboardingStatus.EXPIRED);

  private final Map<OnboardingStatus, Set<OnboardingStatus>> automatic;

  public OnboardingTransitionPolicy() {
    this.automatic = buildAutomaticTransitions();
  }

  /**
   * @param administrative when {@code true}, allows {@code BLOCKED} → {@code INVITED} or {@code
   *     OTP_PENDING} in addition to automatic transitions.
   * @throws IllegalStateException if the transition is not allowed
   */
  public void validate(OnboardingStatus from, OnboardingStatus to, boolean administrative) {
    if (from == null) {
      throw new IllegalStateException("Current status must be set before transition");
    }
    if (to == null) {
      throw new IllegalStateException("Target status must be set");
    }
    if (from == to) {
      return;
    }
    if (TERMINAL.contains(from)) {
      throw new IllegalStateException("Cannot transition from terminal status: " + from);
    }
    if (administrative
        && from == OnboardingStatus.BLOCKED
        && (to == OnboardingStatus.INVITED || to == OnboardingStatus.OTP_PENDING)) {
      return;
    }
    if (administrative
        && from == OnboardingStatus.AML_REJECTED
        && to == OnboardingStatus.AML_CHECK_PENDING) {
      return;
    }
    Set<OnboardingStatus> allowed = automatic.get(from);
    if (allowed != null && allowed.contains(to)) {
      return;
    }
    throw new IllegalStateException(
        "Illegal onboarding transition: " + from + " -> " + to
            + (administrative ? "" : " (non-administrative)"));
  }

  private static Map<OnboardingStatus, Set<OnboardingStatus>> buildAutomaticTransitions() {
    EnumMap<OnboardingStatus, Set<OnboardingStatus>> m = new EnumMap<>(OnboardingStatus.class);

    put(m, OnboardingStatus.UPLOADED, OnboardingStatus.INVALID, OnboardingStatus.VALIDATED);
    put(m, OnboardingStatus.INVALID, OnboardingStatus.VALIDATED);
    put(m, OnboardingStatus.VALIDATED, OnboardingStatus.INVITED);
    put(m, OnboardingStatus.INVITED, OnboardingStatus.OTP_PENDING, OnboardingStatus.EXPIRED);
    put(
        m,
        OnboardingStatus.OTP_PENDING,
        OnboardingStatus.OTP_VERIFIED,
        OnboardingStatus.EXPIRED,
        OnboardingStatus.BLOCKED);

    put(m, OnboardingStatus.OTP_VERIFIED, OnboardingStatus.OCR_IN_PROGRESS);
    put(m, OnboardingStatus.OCR_IN_PROGRESS, OnboardingStatus.NADRA_PENDING, OnboardingStatus.OCR_FAILED);
    put(m, OnboardingStatus.OCR_FAILED, OnboardingStatus.BLOCKED);

    put(m, OnboardingStatus.NADRA_PENDING, OnboardingStatus.NADRA_VERIFIED, OnboardingStatus.NADRA_FAILED);
    put(m, OnboardingStatus.NADRA_FAILED, OnboardingStatus.BLOCKED);
    put(m, OnboardingStatus.NADRA_VERIFIED, OnboardingStatus.FACE_MATCH_PENDING);

    put(
        m,
        OnboardingStatus.LIVENESS_PENDING,
        OnboardingStatus.LIVENESS_PASSED,
        OnboardingStatus.LIVENESS_FAILED);
    put(m, OnboardingStatus.LIVENESS_FAILED, OnboardingStatus.BLOCKED);
    put(m, OnboardingStatus.LIVENESS_PASSED, OnboardingStatus.FACE_MATCH_PENDING);

    put(
        m,
        OnboardingStatus.FACE_MATCH_PENDING,
        OnboardingStatus.FACE_MATCHED,
        OnboardingStatus.FACE_MATCH_FAILED);
    put(m, OnboardingStatus.FACE_MATCH_FAILED, OnboardingStatus.BLOCKED);
    put(m, OnboardingStatus.FACE_MATCHED, OnboardingStatus.FINGERPRINT_PENDING);

    put(
        m,
        OnboardingStatus.FINGERPRINT_PENDING,
        OnboardingStatus.FINGERPRINT_MATCHED,
        OnboardingStatus.FINGERPRINT_FAILED);
    put(m, OnboardingStatus.FINGERPRINT_FAILED, OnboardingStatus.BLOCKED);
    put(m, OnboardingStatus.FINGERPRINT_MATCHED, OnboardingStatus.QUIZ_PENDING);

    put(m, OnboardingStatus.QUIZ_PENDING, OnboardingStatus.QUIZ_PASSED, OnboardingStatus.QUIZ_FAILED);
    put(m, OnboardingStatus.QUIZ_FAILED, OnboardingStatus.BLOCKED);
    put(m, OnboardingStatus.QUIZ_PASSED, OnboardingStatus.FORM_PENDING);

    put(m, OnboardingStatus.FORM_PENDING, OnboardingStatus.FORM_SUBMITTED);
    put(m, OnboardingStatus.FORM_SUBMITTED, OnboardingStatus.AML_CHECK_PENDING);

    put(m, OnboardingStatus.AML_CHECK_PENDING, OnboardingStatus.T24_PENDING, OnboardingStatus.AML_REJECTED);
    put(m, OnboardingStatus.AML_REJECTED, OnboardingStatus.BLOCKED);

    put(m, OnboardingStatus.T24_PENDING, OnboardingStatus.ACCOUNT_OPENED, OnboardingStatus.T24_FAILED);
    put(m, OnboardingStatus.T24_FAILED, OnboardingStatus.T24_PENDING, OnboardingStatus.BLOCKED);

    return m;
  }

  private static void put(
      EnumMap<OnboardingStatus, Set<OnboardingStatus>> m,
      OnboardingStatus from,
      OnboardingStatus... targets) {
    EnumSet<OnboardingStatus> set = EnumSet.noneOf(OnboardingStatus.class);
    set.addAll(Arrays.asList(targets));
    m.put(from, set);
  }
}
