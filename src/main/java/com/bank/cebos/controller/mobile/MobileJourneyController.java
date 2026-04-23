package com.bank.cebos.controller.mobile;

import com.bank.cebos.dto.mobile.CnicCaptureRequest;
import com.bank.cebos.dto.mobile.FaceMatchSubmitRequest;
import com.bank.cebos.dto.mobile.FingerprintSubmitRequest;
import com.bank.cebos.dto.mobile.FormSchemaResponse;
import com.bank.cebos.dto.mobile.FormSubmitRequest;
import com.bank.cebos.dto.mobile.LivenessSubmitRequest;
import com.bank.cebos.dto.mobile.MpinSetupRequest;
import com.bank.cebos.dto.mobile.MpinSetupResponse;
import com.bank.cebos.dto.mobile.MobileProfileResponse;
import com.bank.cebos.dto.mobile.MobileJourneyStatusResponse;
import com.bank.cebos.dto.mobile.MobilePolicyResponse;
import com.bank.cebos.dto.mobile.QuizSubmitRequest;
import com.bank.cebos.dto.mobile.QuizTemplateResponse;
import com.bank.cebos.dto.mobile.ReviewSubmitRequest;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.mobile.MobileJourneyWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileJourneyController {

  private static final String MOBILE_SECURITY =
      "@principalAccess.hasPrincipalKind(T(com.bank.cebos.enums.PrincipalKind).MOBILE)";

  private final MobileJourneyWorkflowService mobileJourneyWorkflowService;
  private final RuntimeConfigService runtimeConfigService;

  public MobileJourneyController(
      MobileJourneyWorkflowService mobileJourneyWorkflowService,
      RuntimeConfigService runtimeConfigService) {
    this.mobileJourneyWorkflowService = mobileJourneyWorkflowService;
    this.runtimeConfigService = runtimeConfigService;
  }

  @GetMapping("/policy")
  public ResponseEntity<MobilePolicyResponse> policy() {
    return ResponseEntity.ok(
        new MobilePolicyResponse(
            runtimeConfigService.getString("mobile.min_supported_version", "1.0.0"),
            runtimeConfigService.getBoolean("mobile.force_update_enabled", false)));
  }

  @GetMapping("/status")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> status(
      @AuthenticationPrincipal CebosUserDetails principal) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.status(requireMobileId(principal)));
  }

  @GetMapping("/profile")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileProfileResponse> getProfile(
      @AuthenticationPrincipal CebosUserDetails principal) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.getProfile(requireMobileId(principal)));
  }

  @PostMapping("/kyc/cnic/front")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> cnicFront(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody CnicCaptureRequest request) {
    return ResponseEntity.ok(
        mobileJourneyWorkflowService.submitCnicFront(requireMobileId(principal), request));
  }

  @PostMapping("/kyc/cnic/back")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> cnicBack(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody CnicCaptureRequest request) {
    return ResponseEntity.ok(
        mobileJourneyWorkflowService.submitCnicBack(requireMobileId(principal), request));
  }

  @PostMapping("/kyc/liveness/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> liveness(
      @AuthenticationPrincipal CebosUserDetails principal,
      @Valid @RequestBody LivenessSubmitRequest request) {
    return ResponseEntity.ok(
        mobileJourneyWorkflowService.submitLiveness(requireMobileId(principal), request));
  }

  @PostMapping("/kyc/face-match/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> faceMatch(
      @AuthenticationPrincipal CebosUserDetails principal,
      @Valid @RequestBody FaceMatchSubmitRequest request) {
    return ResponseEntity.ok(
        mobileJourneyWorkflowService.submitFaceMatch(requireMobileId(principal), request));
  }

  @PostMapping("/kyc/fingerprint/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> fingerprint(
      @AuthenticationPrincipal CebosUserDetails principal,
      @Valid @RequestBody FingerprintSubmitRequest request) {
    return ResponseEntity.ok(
        mobileJourneyWorkflowService.submitFingerprint(requireMobileId(principal), request));
  }

  @GetMapping("/quiz")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<QuizTemplateResponse> quiz(
      @AuthenticationPrincipal CebosUserDetails principal) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.getQuiz(requireMobileId(principal)));
  }

  @PostMapping("/quiz/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> quizSubmit(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody QuizSubmitRequest request) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.submitQuiz(requireMobileId(principal), request));
  }

  @GetMapping("/form/schema")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<FormSchemaResponse> formSchema(
      @AuthenticationPrincipal CebosUserDetails principal) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.formSchema(requireMobileId(principal)));
  }

  @PostMapping("/form/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> formSubmit(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody FormSubmitRequest request) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.submitForm(requireMobileId(principal), request));
  }

  @PostMapping("/review/submit")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MobileJourneyStatusResponse> reviewSubmit(
      @AuthenticationPrincipal CebosUserDetails principal,
      @Valid @RequestBody ReviewSubmitRequest request) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.submitReview(requireMobileId(principal), request));
  }

  @PostMapping("/mpin/setup")
  @PreAuthorize(MOBILE_SECURITY)
  public ResponseEntity<MpinSetupResponse> setupMpin(
      @AuthenticationPrincipal CebosUserDetails principal, @Valid @RequestBody MpinSetupRequest request) {
    return ResponseEntity.ok(mobileJourneyWorkflowService.setupMpin(requireMobileId(principal), request));
  }

  private static long requireMobileId(CebosUserDetails principal) {
    if (principal == null || principal.kind() != PrincipalKind.MOBILE) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "MOBILE principal required");
    }
    return principal.id();
  }
}
