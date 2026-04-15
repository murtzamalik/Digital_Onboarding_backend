package com.bank.cebos.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.bank.cebos.dto.mobile.CnicCaptureRequest;
import com.bank.cebos.dto.mobile.FaceMatchSubmitRequest;
import com.bank.cebos.dto.mobile.FingerprintSubmitRequest;
import com.bank.cebos.dto.mobile.FormSubmitRequest;
import com.bank.cebos.dto.mobile.LivenessSubmitRequest;
import com.bank.cebos.dto.mobile.QuizSubmitRequest;
import com.bank.cebos.dto.mobile.QuizTemplateResponse;
import com.bank.cebos.dto.mobile.ReviewSubmitRequest;
import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.integration.AmlIntegration;
import com.bank.cebos.integration.NadraIntegration;
import com.bank.cebos.integration.T24Integration;
import com.bank.cebos.integration.model.AmlScreeningResult;
import com.bank.cebos.integration.model.NadraVerificationResult;
import com.bank.cebos.integration.model.T24AccountOpenResult;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.service.config.RuntimeConfigService;
import com.bank.cebos.service.onboarding.EmployeeOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileJourneyWorkflowServiceTest {

  @Mock private EmployeeOnboardingService employeeOnboardingService;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;
  @Mock private RuntimeConfigService runtimeConfigService;
  @Mock private NadraIntegration nadraIntegration;
  @Mock private AmlIntegration amlIntegration;
  @Mock private T24Integration t24Integration;

  private MobileJourneyWorkflowService service;

  @BeforeEach
  void setUp() {
    service =
        new MobileJourneyWorkflowService(
            employeeOnboardingService,
            employeeOnboardingRepository,
            runtimeConfigService,
            nadraIntegration,
            amlIntegration,
            t24Integration,
            new ObjectMapper());
  }

  @Test
  void quizTemplateLoadsFromRuntimeConfig() {
    EmployeeOnboarding e = employee(10L, OnboardingStatus.QUIZ_PENDING);
    when(employeeOnboardingService.getRequiredById(10L)).thenReturn(e);
    when(runtimeConfigService.getString(eq("mobile.quiz.template_json"), any()))
        .thenReturn(
            "{\"templateId\":\"CFG-1\",\"passingScorePercent\":70,\"questions\":[{\"questionId\":\"Q1\",\"prompt\":\"P?\",\"options\":[\"A\",\"B\"]}]}");

    QuizTemplateResponse response = service.quizTemplate(10L);

    assertThat(response.templateId()).isEqualTo("CFG-1");
    assertThat(response.questions()).hasSize(1);
  }

  @Test
  void submitReviewTransitionsToAccountOpenedWhenAmlClearsAndT24Succeeds() {
    EmployeeOnboarding e = employee(11L, OnboardingStatus.FORM_SUBMITTED);
    when(employeeOnboardingService.getRequiredById(11L)).thenReturn(e);
    when(amlIntegration.screen(any())).thenReturn(new AmlScreeningResult(true, "AML-1", "LOW"));
    when(t24Integration.openAccount(any()))
        .thenReturn(new T24AccountOpenResult(true, "ACC-1", "CUST-1", "ok"));

    service.submitReview(11L, new ReviewSubmitRequest("SAVINGS", "PKR"));

    InOrder inOrder = inOrder(employeeOnboardingService);
    inOrder
        .verify(employeeOnboardingService)
        .transition(eq(e), eq(OnboardingStatus.AML_CHECK_PENDING), any(), any());
    inOrder
        .verify(employeeOnboardingService)
        .transition(eq(e), eq(OnboardingStatus.T24_PENDING), any(), any());
    inOrder
        .verify(employeeOnboardingService)
        .transition(eq(e), eq(OnboardingStatus.ACCOUNT_OPENED), any(), any());
    assertThat(e.getAmlCaseReference()).isEqualTo("AML-1");
    assertThat(e.getT24AccountId()).isEqualTo("ACC-1");
  }

  @Test
  void submitReviewTransitionsToAmlRejectedWhenAmlFails() {
    EmployeeOnboarding e = employee(12L, OnboardingStatus.FORM_SUBMITTED);
    when(employeeOnboardingService.getRequiredById(12L)).thenReturn(e);
    when(amlIntegration.screen(any())).thenReturn(new AmlScreeningResult(false, "AML-2", "HIGH"));

    service.submitReview(12L, new ReviewSubmitRequest("SAVINGS", "PKR"));

    InOrder inOrder = inOrder(employeeOnboardingService);
    inOrder
        .verify(employeeOnboardingService)
        .transition(eq(e), eq(OnboardingStatus.AML_CHECK_PENDING), any(), any());
    inOrder
        .verify(employeeOnboardingService)
        .transition(eq(e), eq(OnboardingStatus.AML_REJECTED), any(), any());
  }

  @Test
  void submitQuizUsesConfiguredAnswerKey() {
    EmployeeOnboarding e = employee(13L, OnboardingStatus.QUIZ_PENDING);
    when(employeeOnboardingService.getRequiredById(13L)).thenReturn(e);
    when(runtimeConfigService.getString(eq("mobile.quiz.answer_key_json"), any()))
        .thenReturn("{\"Q1\":\"RIGHT\"}");
    QuizSubmitRequest req =
        new QuizSubmitRequest(
            "T1", List.of(new com.bank.cebos.dto.mobile.QuizAnswerRequest("Q1", "this is right answer")));

    service.submitQuiz(13L, req);

    assertThat(e.getQuizScore()).isEqualTo(1);
    assertThat(e.getQuizPassed()).isFalse();
  }

  @Test
  void smokeJourneyTransitionsFromOtpVerifiedToAccountOpened() {
    EmployeeOnboarding e = employee(20L, OnboardingStatus.OTP_VERIFIED);
    when(employeeOnboardingService.getRequiredById(20L)).thenReturn(e);
    doAnswer(
            inv -> {
              EmployeeOnboarding target = inv.getArgument(0);
              OnboardingStatus next = inv.getArgument(1);
              target.setStatus(next);
              return null;
            })
        .when(employeeOnboardingService)
        .transition(any(EmployeeOnboarding.class), any(OnboardingStatus.class), any(), any());
    when(amlIntegration.screen(any())).thenReturn(new AmlScreeningResult(true, "AML-SMOKE", "LOW"));
    when(t24Integration.openAccount(any()))
        .thenReturn(new T24AccountOpenResult(true, "ACC-SMOKE", "CUST-SMOKE", "ok"));
    when(nadraIntegration.verifyByCnic(any()))
        .thenReturn(new NadraVerificationResult(true, "NADRA-SMOKE", "OK"));
    when(runtimeConfigService.getString(eq("mobile.quiz.answer_key_json"), any()))
        .thenReturn("{\"Q1\":\"A\",\"Q2\":\"B\",\"Q3\":\"C\"}");

    service.submitCnicFront(20L, new CnicCaptureRequest("mobile://front.jpg"));
    service.submitCnicBack(20L, new CnicCaptureRequest("mobile://back.jpg"));
    service.submitLiveness(
        20L, new LivenessSubmitRequest("L1", "MOBILE", new BigDecimal("0.91"), "PASSED"));
    service.submitFaceMatch(
        20L, new FaceMatchSubmitRequest("mobile://selfie.jpg", new BigDecimal("0.90"), "MATCHED"));
    service.submitFingerprint(
        20L,
        new FingerprintSubmitRequest("TPL-1", "mobile://fp.dat", new BigDecimal("0.95"), "MATCHED"));
    service.submitQuiz(
        20L,
        new QuizSubmitRequest(
            "T-SMOKE",
            List.of(
                new com.bank.cebos.dto.mobile.QuizAnswerRequest("Q1", "A"),
                new com.bank.cebos.dto.mobile.QuizAnswerRequest("Q2", "B"),
                new com.bank.cebos.dto.mobile.QuizAnswerRequest("Q3", "C"))));
    service.submitForm(20L, new FormSubmitRequest(Map.of("fullName", "Smoke User")));
    service.submitReview(20L, new ReviewSubmitRequest("SAVINGS", "PKR"));

    assertThat(e.getStatus()).isEqualTo(OnboardingStatus.ACCOUNT_OPENED);
  }

  private static EmployeeOnboarding employee(long id, OnboardingStatus status) {
    EmployeeOnboarding e = new EmployeeOnboarding();
    e.setId(id);
    e.setEmployeeRef("EMP-" + id);
    e.setStatus(status);
    e.setCnic("35202-1234567-1");
    e.setFullName("Employee " + id);
    e.setCorporateClientId(1L);
    e.setPresentCountry("PK");
    return e;
  }
}
