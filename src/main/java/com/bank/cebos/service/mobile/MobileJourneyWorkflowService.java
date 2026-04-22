package com.bank.cebos.service.mobile;

import com.bank.cebos.dto.mobile.CnicCaptureRequest;
import com.bank.cebos.dto.mobile.FaceMatchSubmitRequest;
import com.bank.cebos.dto.mobile.FingerprintSubmitRequest;
import com.bank.cebos.dto.mobile.FormFieldResponse;
import com.bank.cebos.dto.mobile.FormSchemaResponse;
import com.bank.cebos.dto.mobile.FormSubmitRequest;
import com.bank.cebos.dto.mobile.LivenessSubmitRequest;
import com.bank.cebos.dto.mobile.MobileJourneyStatusResponse;
import com.bank.cebos.dto.mobile.QuizQuestionResponse;
import com.bank.cebos.dto.mobile.QuizSubmitRequest;
import com.bank.cebos.dto.mobile.QuizTemplateResponse;
import com.bank.cebos.dto.mobile.ReviewSubmitRequest;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.NadraIntegration;
import com.bank.cebos.integration.T24Integration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import com.bank.cebos.integration.model.NadraVerificationResult;
import com.bank.cebos.integration.model.T24AccountOpenCommand;
import com.bank.cebos.integration.model.T24AccountOpenResult;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MobileJourneyWorkflowService {

  private static final String DEFAULT_QUIZ_TEMPLATE = "CEBOS-QZ-1";
  private static final int QUIZ_MAX_SCORE = 3;
  private static final int QUIZ_PASSING_SCORE = 2;
  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};

  private final EmployeeOnboardingService employeeOnboardingService;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final RuntimeConfigService runtimeConfigService;
  private final NadraIntegration nadraIntegration;
  private final AmlIntegration amlIntegration;
  private final T24Integration t24Integration;
  private final ObjectMapper objectMapper;

  public MobileJourneyWorkflowService(
      EmployeeOnboardingService employeeOnboardingService,
      EmployeeOnboardingRepository employeeOnboardingRepository,
      RuntimeConfigService runtimeConfigService,
      NadraIntegration nadraIntegration,
      AmlIntegration amlIntegration,
      T24Integration t24Integration,
      ObjectMapper objectMapper) {
    this.employeeOnboardingService = employeeOnboardingService;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.runtimeConfigService = runtimeConfigService;
    this.nadraIntegration = nadraIntegration;
    this.amlIntegration = amlIntegration;
    this.t24Integration = t24Integration;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public MobileJourneyStatusResponse status(long onboardingId) {
    return toResponse(employeeOnboardingService.getRequiredById(onboardingId));
  }

  @Transactional
  public MobileJourneyStatusResponse submitCnicFront(long onboardingId, CnicCaptureRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(
        e, EnumSet.of(OnboardingStatus.OTP_VERIFIED, OnboardingStatus.OCR_IN_PROGRESS));
    if (e.getStatus() == OnboardingStatus.OTP_VERIFIED) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.OCR_IN_PROGRESS,
          changedBy(onboardingId),
          "Mobile: entering OCR stage from OTP verified");
    }
    e.setCnicFrontImagePath(request.imagePath().trim());
    e.setOcrStatus("CAPTURED_FRONT");
    e.setOcrJobId("OCR-" + e.getEmployeeRef() + "-" + Instant.now().toEpochMilli());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        OnboardingStatus.NADRA_PENDING,
        changedBy(onboardingId),
        "Mobile: CNIC front captured");
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitCnicBack(long onboardingId, CnicCaptureRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.NADRA_PENDING);
    e.setCnicBackImagePath(request.imagePath().trim());
    NadraVerificationResult nadra = nadraIntegration.verifyByCnic(e.getCnic());
    e.setNadraTransactionId(nadra.verificationReference());
    e.setNadraVerificationCode(nadra.statusCode());
    e.setNadraVerificationStatus(nadra.verified() ? "VERIFIED" : "FAILED");
    e.setNadraResponsePayload(toJson(Map.of("verified", nadra.verified(), "statusCode", nadra.statusCode())));
    e.setNadraVerifiedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        nadra.verified() ? OnboardingStatus.NADRA_VERIFIED : OnboardingStatus.NADRA_FAILED,
        changedBy(onboardingId),
        "Mobile: NADRA verification completed");
    if (nadra.verified()) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.LIVENESS_PENDING,
          changedBy(onboardingId),
          "auto-advance after NADRA verification");
    }
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitLiveness(long onboardingId, LivenessSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.LIVENESS_PENDING);
    boolean passed = isPositive(request.result());
    e.setLivenessSessionId(request.sessionId().trim());
    e.setLivenessVendorRef(request.vendorRef().trim());
    e.setLivenessScore(request.score());
    e.setLivenessResult(passed ? "PASSED" : "FAILED");
    e.setLivenessCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        passed ? OnboardingStatus.LIVENESS_PASSED : OnboardingStatus.LIVENESS_FAILED,
        changedBy(onboardingId),
        "Mobile: liveness submitted");
    if (passed) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.FACE_MATCH_PENDING,
          changedBy(onboardingId),
          "auto-advance after liveness passed");
    }
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitFaceMatch(long onboardingId, FaceMatchSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.FACE_MATCH_PENDING);
    boolean passed = isPositive(request.result());
    e.setSelfieImagePath(request.selfieImagePath().trim());
    e.setFaceMatchScore(request.score());
    e.setFaceMatchResult(passed ? "MATCHED" : "FAILED");
    e.setFaceMatchCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        passed ? OnboardingStatus.FACE_MATCHED : OnboardingStatus.FACE_MATCH_FAILED,
        changedBy(onboardingId),
        "Mobile: face match submitted");
    if (passed) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.FINGERPRINT_PENDING,
          changedBy(onboardingId),
          "auto-advance after face matched");
    }
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitFingerprint(long onboardingId, FingerprintSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.FINGERPRINT_PENDING);
    boolean passed = isPositive(request.result());
    e.setFingerprintTemplateRef(request.templateRef().trim());
    e.setFingerprintCapturePath(request.capturePath().trim());
    e.setFingerprintQualityScore(request.qualityScore());
    e.setFingerprintMatchResult(passed ? "MATCHED" : "FAILED");
    e.setFingerprintCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        passed ? OnboardingStatus.FINGERPRINT_MATCHED : OnboardingStatus.FINGERPRINT_FAILED,
        changedBy(onboardingId),
        "Mobile: fingerprint submitted");
    if (passed) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.QUIZ_PENDING,
          changedBy(onboardingId),
          "auto-advance after fingerprint matched");
    }
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public QuizTemplateResponse quizTemplate(long onboardingId) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.QUIZ_PENDING);
    return loadQuizTemplate();
  }

  @Transactional
  public MobileJourneyStatusResponse submitQuiz(long onboardingId, QuizSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.QUIZ_PENDING);
    int score = scoreQuiz(request);
    boolean passed = score >= QUIZ_PASSING_SCORE;
    e.setQuizTemplateId(request.templateId().trim());
    e.setQuizScore(score);
    e.setQuizMaxScore(QUIZ_MAX_SCORE);
    e.setQuizPassed(passed);
    e.setQuizAnswersJson(toJson(request.answers()));
    e.setQuizCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        passed ? OnboardingStatus.QUIZ_PASSED : OnboardingStatus.QUIZ_FAILED,
        changedBy(onboardingId),
        "Mobile: quiz submitted");
    if (passed) {
      employeeOnboardingService.transition(
          e, OnboardingStatus.FORM_PENDING, changedBy(onboardingId), "auto-advance after quiz passed");
    }
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public FormSchemaResponse formSchema(long onboardingId) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.FORM_PENDING);
    return loadFormSchema();
  }

  @Transactional
  public MobileJourneyStatusResponse submitForm(long onboardingId, FormSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.FORM_PENDING);
    e.setFormDataJson(toJson(request.data()));
    e.setFormSubmittedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        OnboardingStatus.FORM_SUBMITTED,
        changedBy(onboardingId),
        "Mobile: form submitted");
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitReview(long onboardingId, ReviewSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.FORM_SUBMITTED);

    employeeOnboardingService.transition(
        e,
        OnboardingStatus.AML_CHECK_PENDING,
        changedBy(onboardingId),
        "Mobile: review submitted, AML started");

    AmlScreeningResult aml =
        amlIntegration.screen(
            new AmlScreeningRequest(
                e.getFullName(), e.getCnic(), e.getPresentCountry() != null ? e.getPresentCountry() : "PK"));
    e.setAmlCaseReference(aml.screeningReference());
    e.setAmlScreeningStatus(aml.cleared() ? "CLEARED" : "REJECTED");
    e.setAmlScreeningSummary("riskBand=" + aml.riskBand());
    e.setAmlLastCheckedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    if (!aml.cleared()) {
        employeeOnboardingService.transition(
            e,
            OnboardingStatus.AML_REJECTED,
            changedBy(onboardingId),
            "Mobile: AML screening rejected");
      return toResponse(e);
    }

    employeeOnboardingService.transition(
        e, OnboardingStatus.T24_PENDING, changedBy(onboardingId), "Mobile: AML cleared, T24 started");

    T24AccountOpenResult t24 =
        t24Integration.openAccount(
            new T24AccountOpenCommand(
                e.getCnic(),
                request.productCode().trim().toUpperCase(Locale.ROOT),
                request.currency().trim().toUpperCase(Locale.ROOT)));
    e.setT24SubmissionStatus(t24.success() ? "SUCCESS" : "FAILED");
    e.setT24LastAttemptAt(Instant.now());
    e.setT24LastError(t24.success() ? null : t24.message());
    e.setT24AccountId(t24.accountNumber());
    e.setT24CustomerId(t24.t24CustomerId());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        t24.success() ? OnboardingStatus.ACCOUNT_OPENED : OnboardingStatus.T24_FAILED,
        changedBy(onboardingId),
        "Mobile: T24 account opening completed");
    return toResponse(e);
  }

  private int scoreQuiz(QuizSubmitRequest request) {
    Map<String, String> answerKey = loadQuizAnswerKey();
    int score = 0;
    for (var answer : request.answers()) {
      String q = answer.questionId().trim().toUpperCase(Locale.ROOT);
      String a = answer.answer().trim().toUpperCase(Locale.ROOT);
      String expectedContains = answerKey.get(q);
      if (expectedContains != null && a.contains(expectedContains.toUpperCase(Locale.ROOT))) {
        score++;
      }
    }
    return score;
  }

  private QuizTemplateResponse loadQuizTemplate() {
    String fallback =
        """
        {
          "templateId":"CEBOS-QZ-1",
          "passingScorePercent":67,
          "questions":[
            {"questionId":"Q1","prompt":"What is your onboarding employee reference?","options":["Stored on invite","Always your CNIC","Always your mobile"]},
            {"questionId":"Q2","prompt":"Which channel issues your invite?","options":["Corporate onboarding","ATM machine","Physical branch only"]},
            {"questionId":"Q3","prompt":"When should OTP be shared?","options":["Never","With any caller","On social media"]}
          ]
        }
        """;
    String raw = runtimeConfigService.getString("mobile.quiz.template_json", fallback);
    try {
      return objectMapper.readValue(raw, QuizTemplateResponse.class);
    } catch (JsonProcessingException ignored) {
      return new QuizTemplateResponse(
          DEFAULT_QUIZ_TEMPLATE,
          67,
          List.of(
              new QuizQuestionResponse(
                  "Q1",
                  "What is your onboarding employee reference?",
                  List.of("Stored on invite", "Always your CNIC", "Always your mobile")),
              new QuizQuestionResponse(
                  "Q2",
                  "Which channel issues your invite?",
                  List.of("Corporate onboarding", "ATM machine", "Physical branch only")),
              new QuizQuestionResponse(
                  "Q3",
                  "When should OTP be shared?",
                  List.of("Never", "With any caller", "On social media"))));
    }
  }

  private FormSchemaResponse loadFormSchema() {
    String fallback =
        """
        {
          "templateId":"CEBOS-FORM-1",
          "fields":[
            {"key":"fullName","label":"Full Name","inputType":"text","required":true},
            {"key":"email","label":"Email","inputType":"email","required":true},
            {"key":"motherName","label":"Mother Name","inputType":"text","required":true},
            {"key":"presentAddress","label":"Present Address","inputType":"text","required":true},
            {"key":"permanentAddress","label":"Permanent Address","inputType":"text","required":true}
          ]
        }
        """;
    String raw = runtimeConfigService.getString("mobile.form.schema_json", fallback);
    try {
      return objectMapper.readValue(raw, FormSchemaResponse.class);
    } catch (JsonProcessingException ignored) {
      return new FormSchemaResponse(
          "CEBOS-FORM-1",
          List.of(
              new FormFieldResponse("fullName", "Full Name", "text", true),
              new FormFieldResponse("email", "Email", "email", true),
              new FormFieldResponse("motherName", "Mother Name", "text", true),
              new FormFieldResponse("presentAddress", "Present Address", "text", true),
              new FormFieldResponse("permanentAddress", "Permanent Address", "text", true)));
    }
  }

  private Map<String, String> loadQuizAnswerKey() {
    String fallback = "{\"Q1\":\"INVITE\",\"Q2\":\"CORPORATE\",\"Q3\":\"NEVER\"}";
    String raw = runtimeConfigService.getString("mobile.quiz.answer_key_json", fallback);
    try {
      Map<String, String> parsed = objectMapper.readValue(raw, STRING_MAP_TYPE);
      return parsed != null ? parsed : Collections.emptyMap();
    } catch (JsonProcessingException ignored) {
      return Map.of("Q1", "INVITE", "Q2", "CORPORATE", "Q3", "NEVER");
    }
  }

  private static boolean isPositive(String result) {
    String normalized = result.trim().toUpperCase(Locale.ROOT);
    return "PASS".equals(normalized)
        || "PASSED".equals(normalized)
        || "MATCHED".equals(normalized)
        || "TRUE".equals(normalized)
        || "YES".equals(normalized);
  }

  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload");
    }
  }

  private static String changedBy(long onboardingId) {
    return "mobile:" + onboardingId;
  }

  private static MobileJourneyStatusResponse toResponse(EmployeeOnboarding e) {
    return new MobileJourneyStatusResponse(e.getId(), e.getEmployeeRef(), e.getStatus(), e.getCorporateClientId());
  }
}
