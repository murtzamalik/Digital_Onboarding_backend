package com.bank.cebos.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.cebos.enums.OnboardingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingTransitionPolicyTest {

  private OnboardingTransitionPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new OnboardingTransitionPolicy();
  }

  @Test
  void allowsOtpVerifiedToOcrInProgress() {
    assertThatCode(() -> policy.validate(OnboardingStatus.OTP_VERIFIED, OnboardingStatus.OCR_IN_PROGRESS, false))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsSkipToFormPendingFromOtpVerified() {
    assertThatThrownBy(() -> policy.validate(OnboardingStatus.OTP_VERIFIED, OnboardingStatus.FORM_PENDING, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Illegal onboarding transition")
        .hasMessageContaining("OTP_VERIFIED")
        .hasMessageContaining("FORM_PENDING");
  }

  @Test
  void terminalAccountOpenedCannotMove() {
    assertThatThrownBy(() -> policy.validate(OnboardingStatus.ACCOUNT_OPENED, OnboardingStatus.OCR_IN_PROGRESS, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("terminal");
  }

  @Test
  void administrativeAllowsBlockedToInvited() {
    assertThatCode(() -> policy.validate(OnboardingStatus.BLOCKED, OnboardingStatus.INVITED, true))
        .doesNotThrowAnyException();
  }

  @Test
  void nonAdministrativeBlocksBlockedToInvited() {
    assertThatThrownBy(() -> policy.validate(OnboardingStatus.BLOCKED, OnboardingStatus.INVITED, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Illegal onboarding transition");
  }

  @Test
  void administrativeAllowsAmlRejectedToAmlCheckPending() {
    assertThatCode(
            () ->
                policy.validate(
                    OnboardingStatus.AML_REJECTED, OnboardingStatus.AML_CHECK_PENDING, true))
        .doesNotThrowAnyException();
  }

  @Test
  void nonAdministrativeBlocksAmlRejectedToAmlCheckPending() {
    assertThatThrownBy(
            () ->
                policy.validate(
                    OnboardingStatus.AML_REJECTED, OnboardingStatus.AML_CHECK_PENDING, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Illegal onboarding transition");
  }
}
