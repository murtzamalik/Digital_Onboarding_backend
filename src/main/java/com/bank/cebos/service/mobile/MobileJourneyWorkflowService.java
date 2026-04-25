package com.bank.cebos.service.mobile;

import com.bank.cebos.config.BbsKycProperties;
import com.bank.cebos.dto.mobile.CnicCaptureRequest;
import com.bank.cebos.dto.mobile.FaceMatchSubmitRequest;
import com.bank.cebos.dto.mobile.FingerprintSubmitRequest;
import com.bank.cebos.dto.mobile.FormFieldResponse;
import com.bank.cebos.dto.mobile.FormSchemaResponse;
import com.bank.cebos.dto.mobile.FormSubmitRequest;
import com.bank.cebos.dto.mobile.LivenessSubmitRequest;
import com.bank.cebos.dto.mobile.MpinSetupRequest;
import com.bank.cebos.dto.mobile.MpinSetupResponse;
import com.bank.cebos.dto.mobile.MobileProfileResponse;
import com.bank.cebos.dto.mobile.MobileJourneyStatusResponse;
import com.bank.cebos.dto.mobile.QuizAnswerRequest;
import com.bank.cebos.dto.mobile.QuizQuestionResponse;
import com.bank.cebos.dto.mobile.QuizSubmitRequest;
import com.bank.cebos.dto.mobile.QuizTemplateResponse;
import com.bank.cebos.dto.mobile.ReviewSubmitRequest;
import com.bank.cebos.entity.CorporateClient;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.bbs.BbsFaceMatchResult;
import com.bank.cebos.integration.bbs.BbsKycClient;
import com.bank.cebos.integration.bbs.PakistaniCnicBbsResponseMapper;
import com.bank.cebos.integration.NadraIntegration;
import com.bank.cebos.integration.T24Integration;
import com.bank.cebos.integration.model.AmlScreeningRequest;
import com.bank.cebos.integration.model.AmlScreeningResult;
import com.bank.cebos.integration.model.NadraVerificationResult;
import com.bank.cebos.integration.model.T24AccountOpenCommand;
import com.bank.cebos.integration.model.T24AccountOpenResult;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.bank.cebos.util.Base64ImagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Base64;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MobileJourneyWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(MobileJourneyWorkflowService.class);
  private static final String MOTHER_NAME_QUIZ_TEMPLATE_ID = "MOTHER_NAME_QUIZ";
  private static final String MOTHER_NAME_QUESTION_ID = "Q_MOTHER_NAME";
  private static final String MOTHER_NAME_QUESTION_TEXT = "What is your mother's name?";
  private static final List<String> MOTHER_NAME_DECOY_POOL =
      List.of(
          "Fatima Bibi",
          "Zainab Begum",
          "Amina Khatoon",
          "Nazia Parveen",
          "Razia Sultana",
          "Sadia Bano",
          "Hina Malik",
          "Rukhsana Akhtar",
          "Shahida Naz",
          "Mehnaz Begum");

  private final EmployeeOnboardingService employeeOnboardingService;
  private final EmployeeOnboardingRepository employeeOnboardingRepository;
  private final CorporateClientRepository corporateClientRepository;
  private final RuntimeConfigService runtimeConfigService;
  private final NadraIntegration nadraIntegration;
  private final AmlIntegration amlIntegration;
  private final T24Integration t24Integration;
  private final BbsKycClient bbsKycClient;
  private final BbsKycProperties bbsKycProperties;
  private final ObjectMapper objectMapper;

  public MobileJourneyWorkflowService(
      EmployeeOnboardingService employeeOnboardingService,
      EmployeeOnboardingRepository employeeOnboardingRepository,
      CorporateClientRepository corporateClientRepository,
      RuntimeConfigService runtimeConfigService,
      NadraIntegration nadraIntegration,
      AmlIntegration amlIntegration,
      T24Integration t24Integration,
      BbsKycClient bbsKycClient,
      BbsKycProperties bbsKycProperties,
      ObjectMapper objectMapper) {
    this.employeeOnboardingService = employeeOnboardingService;
    this.employeeOnboardingRepository = employeeOnboardingRepository;
    this.corporateClientRepository = corporateClientRepository;
    this.runtimeConfigService = runtimeConfigService;
    this.nadraIntegration = nadraIntegration;
    this.amlIntegration = amlIntegration;
    this.t24Integration = t24Integration;
    this.bbsKycClient = bbsKycClient;
    this.bbsKycProperties = bbsKycProperties;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public MobileJourneyStatusResponse status(long onboardingId) {
    return toResponse(employeeOnboardingService.getRequiredById(onboardingId));
  }

  @Transactional(readOnly = true)
  public MobileProfileResponse getProfile(long onboardingId) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    CorporateClient client = resolveCorporateClient(e.getCorporateClientId());
    return new MobileProfileResponse(
        e.getId(),
        e.getEmployeeRef(),
        e.getStatus().name(),
        e.getFullName(),
        e.getFatherName(),
        e.getCnic(),
        e.getMobile(),
        e.getGender(),
        e.getDateOfBirth() != null ? e.getDateOfBirth().toString() : null,
        e.getCnicIssueDate() != null ? e.getCnicIssueDate().toString() : null,
        e.getCnicExpiryDate() != null ? e.getCnicExpiryDate().toString() : null,
        e.getPresentAddressLine1(),
        dashIfBlank(client != null ? client.getLegalName() : null),
        dashIfBlank(client != null ? client.getClientCode() : null),
        dashIfBlank(client != null ? client.getTradeName() : null),
        dashIfBlank(client != null ? client.getIndustry() : null),
        dashIfBlank(client != null ? client.getRegisteredAddress() : null),
        dashIfBlank(client != null ? client.getCity() : null),
        dashIfBlank(client != null ? client.getContactPhone() : null),
        dashIfBlank(client != null ? client.getContactEmail() : null),
        dashIfBlank(client != null ? client.getCompanyRegistrationNo() : null),
        "Employee",
        "Salary",
        "Savings / Salary Account");
  }

  private CorporateClient resolveCorporateClient(Long corporateClientId) {
    if (corporateClientId == null) {
      return null;
    }
    return corporateClientRepository.findById(corporateClientId).orElse(null);
  }

  private static String dashIfBlank(String value) {
    if (value == null) {
      return "—";
    }
    String t = value.trim();
    return t.isEmpty() ? "—" : t;
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
    byte[] frontBytes;
    try {
      frontBytes = Base64ImagePayload.decodeToBytes(request.base64Image());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    JsonNode bbs = bbsKycClient.extractPakistaniIdCard(request.base64Image());
    PakistaniCnicBbsResponseMapper.mergeBbsOcrKey(e, "front", bbs, objectMapper);
    PakistaniCnicBbsResponseMapper.OcrTextFields ocr = PakistaniCnicBbsResponseMapper.toOcrTextFields(bbs);
    applyBbsOcrToEntity(e, ocr);
    e.setCnicFrontImageData(frontBytes);
    e.setCnicFrontImagePath(null);
    e.setOcrStatus("CAPTURED_FRONT");
    e.setOcrJobId("OCR-BBS-" + e.getEmployeeRef() + "-" + Instant.now().toEpochMilli());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        OnboardingStatus.NADRA_PENDING,
        changedBy(onboardingId),
        "Mobile: CNIC front captured (BBS OCR)");
    return toResponse(e);
  }

  @Transactional
  public MobileJourneyStatusResponse submitCnicBack(long onboardingId, CnicCaptureRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.NADRA_PENDING);
    byte[] backBytes;
    try {
      backBytes = Base64ImagePayload.decodeToBytes(request.base64Image());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    JsonNode bbs = bbsKycClient.extractPakistaniIdCard(request.base64Image());
    PakistaniCnicBbsResponseMapper.mergeBbsOcrKey(e, "back", bbs, objectMapper);
    PakistaniCnicBbsResponseMapper.OcrTextFields ocr = PakistaniCnicBbsResponseMapper.toOcrTextFields(bbs);
    applyBbsOcrToEntity(e, ocr);
    e.setCnicBackImageData(backBytes);
    e.setCnicBackImagePath(null);
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
          OnboardingStatus.FACE_MATCH_PENDING,
          changedBy(onboardingId),
          "auto-advance after NADRA verification (face match next)");
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
    byte[] frontBytes = e.getCnicFrontImageData();
    if (frontBytes == null || frontBytes.length == 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "CNIC front image is not available for face match");
    }
    String idCardB64 = Base64.getEncoder().encodeToString(frontBytes);
    byte[] selfieBytes;
    try {
      selfieBytes = Base64ImagePayload.decodeToBytes(request.selfieBase64());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    BbsFaceMatchResult r = bbsKycClient.matchIdCardToSelfie(idCardB64, request.selfieBase64());
    double similarity = r.similarity();
    boolean passed =
        r.match() && similarity >= bbsKycProperties.faceMatchMinSimilarity();
    e.setSelfieImageData(selfieBytes);
    e.setSelfieImagePath(null);
    e.setFaceMatchScore(
        BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP));
    e.setFaceMatchResult(passed ? "MATCHED" : "FAILED");
    e.setFaceMatchCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);
    employeeOnboardingService.transition(
        e,
        passed ? OnboardingStatus.FACE_MATCHED : OnboardingStatus.FACE_MATCH_FAILED,
        changedBy(onboardingId),
        "Mobile: face match (BBS) similarity=" + similarity);
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
  public QuizTemplateResponse getQuiz(long onboardingId) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.QUIZ_PENDING);
    String correctAnswer = requireMotherName(e);
    List<String> decoys =
        MOTHER_NAME_DECOY_POOL.stream()
            .filter(d -> !d.equalsIgnoreCase(correctAnswer))
            .collect(
                java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toList(),
                    list -> {
                      java.util.Collections.shuffle(list);
                      return list.subList(0, Math.min(2, list.size()));
                    }));

    List<String> options = new ArrayList<>(decoys);
    options.add(correctAnswer);
    java.util.Collections.shuffle(options);

    QuizQuestionResponse question =
        new QuizQuestionResponse(MOTHER_NAME_QUESTION_ID, MOTHER_NAME_QUESTION_TEXT, options);
    return new QuizTemplateResponse(MOTHER_NAME_QUIZ_TEMPLATE_ID, 100, List.of(question));
  }

  @Transactional(readOnly = true)
  public QuizTemplateResponse quizTemplate(long onboardingId) {
    return getQuiz(onboardingId);
  }

  @Transactional
  public MobileJourneyStatusResponse submitQuiz(long onboardingId, QuizSubmitRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    employeeOnboardingService.requireCurrentStatus(e, OnboardingStatus.QUIZ_PENDING);
    String correctAnswer = requireMotherName(e);
    String submitted =
        request.answers().stream()
            .filter(a -> MOTHER_NAME_QUESTION_ID.equals(a.questionId()))
            .map(QuizAnswerRequest::answer)
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Answer for Q_MOTHER_NAME is required"));

    boolean passed = submitted.trim().equalsIgnoreCase(correctAnswer);
    e.setQuizTemplateId(MOTHER_NAME_QUIZ_TEMPLATE_ID);
    e.setQuizScore(passed ? 1 : 0);
    e.setQuizMaxScore(1);
    e.setQuizPassed(passed);
    e.setQuizAnswersJson(toJson(Map.of(MOTHER_NAME_QUESTION_ID, submitted)));
    e.setQuizCompletedAt(Instant.now());
    employeeOnboardingRepository.save(e);

    if (!passed) {
      employeeOnboardingService.transition(
          e,
          OnboardingStatus.QUIZ_FAILED,
          changedBy(onboardingId),
          "Mobile: quiz failed - wrong mother name");
      return toResponse(e);
    }

    employeeOnboardingService.transition(
        e, OnboardingStatus.QUIZ_PASSED, changedBy(onboardingId), "Mobile: quiz passed");
    employeeOnboardingService.transition(
        e, OnboardingStatus.FORM_PENDING, changedBy(onboardingId), "Mobile: auto-advance to FORM_PENDING");
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

  @Transactional
  public MpinSetupResponse setupMpin(long onboardingId, MpinSetupRequest request) {
    EmployeeOnboarding e = employeeOnboardingService.getRequiredById(onboardingId);
    Set<OnboardingStatus> allowed = EnumSet.noneOf(OnboardingStatus.class);
    for (OnboardingStatus status : OnboardingStatus.values()) {
      if (status.name().contains("AML")
          || status.name().contains("T24")
          || status.name().contains("ACCOUNT")
          || status.name().contains("COMPLETE")
          || status.name().contains("ACTIVE")
          || status.name().contains("REVIEW_SUBMITTED")) {
        allowed.add(status);
      }
    }
    if (!allowed.contains(e.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "MPIN can only be set after review is submitted. Current status: " + e.getStatus());
    }
    if (e.getMpinHash() != null && !e.getMpinHash().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "MPIN already set");
    }
    String hash = new BCryptPasswordEncoder().encode(request.mpin());
    e.setMpinHash(hash);
    employeeOnboardingRepository.save(e);
    return new MpinSetupResponse(true, "MPIN set successfully");
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

  private static String requireMotherName(EmployeeOnboarding e) {
    if (e.getMotherName() == null || e.getMotherName().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Mother name not available - OCR may not have extracted it");
    }
    return e.getMotherName().trim();
  }

  private static boolean isPositive(String result) {
    String normalized = result.trim().toUpperCase(Locale.ROOT);
    return "PASS".equals(normalized)
        || "PASSED".equals(normalized)
        || "MATCHED".equals(normalized)
        || "TRUE".equals(normalized)
        || "YES".equals(normalized);
  }

  private void applyBbsOcrToEntity(
      EmployeeOnboarding e, PakistaniCnicBbsResponseMapper.OcrTextFields o) {
    if (o == null) {
      return;
    }
    if (StringUtils.hasText(o.fullName())
        && (e.getFullName() == null || e.getFullName().isBlank())) {
      e.setFullName(o.fullName().trim());
    }
    if (StringUtils.hasText(o.fatherName())
        && (e.getFatherName() == null || e.getFatherName().isBlank())) {
      e.setFatherName(o.fatherName().trim());
    }
    if (StringUtils.hasText(o.motherName())
        && (e.getMotherName() == null || e.getMotherName().isBlank())) {
      e.setMotherName(o.motherName().trim());
    }
    if (StringUtils.hasText(o.cnic()) && (e.getCnic() == null || e.getCnic().isBlank())) {
      e.setCnic(o.cnic().trim());
    }
    if (StringUtils.hasText(o.gender()) && (e.getGender() == null || e.getGender().isBlank())) {
      e.setGender(o.gender().trim());
    }
    if (e.getDateOfBirth() == null) {
      tryParseAndSetDate(o.dob(), e::setDateOfBirth);
    }
    if (e.getCnicIssueDate() == null) {
      tryParseAndSetDate(o.issueDate(), e::setCnicIssueDate);
    }
    if (e.getCnicExpiryDate() == null) {
      tryParseAndSetDate(o.expiryDate(), e::setCnicExpiryDate);
    }
    if (StringUtils.hasText(o.address())
        && (e.getPresentAddressLine1() == null || e.getPresentAddressLine1().isBlank())) {
      e.setPresentAddressLine1(o.address().trim());
    }
  }

  private void tryParseAndSetDate(String raw, Consumer<LocalDate> setter) {
    if (raw == null || raw.isBlank()) {
      return;
    }
    for (String pattern : List.of("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "MM/dd/yyyy")) {
      try {
        setter.accept(LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern(pattern)));
        return;
      } catch (Exception ignored) {
      }
    }
    log.warn("Could not parse date value '{}' - skipping", raw);
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
