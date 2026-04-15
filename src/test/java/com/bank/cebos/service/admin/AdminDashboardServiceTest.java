package com.bank.cebos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.cebos.dto.admin.AdminDashboardSummaryResponse;
import com.bank.cebos.enums.OnboardingStatus;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.repository.CorrectionBatchRepository;
import com.bank.cebos.repository.EmployeeOnboardingRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

  @Mock private CorporateClientRepository corporateClientRepository;
  @Mock private UploadBatchRepository uploadBatchRepository;
  @Mock private CorrectionBatchRepository correctionBatchRepository;
  @Mock private EmployeeOnboardingRepository employeeOnboardingRepository;

  private AdminDashboardService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminDashboardService(
            corporateClientRepository,
            uploadBatchRepository,
            correctionBatchRepository,
            employeeOnboardingRepository);
  }

  @Test
  void summaryAggregatesCounts() {
    when(corporateClientRepository.count()).thenReturn(3L);
    when(uploadBatchRepository.count()).thenReturn(11L);
    when(correctionBatchRepository.count()).thenReturn(2L);
    when(employeeOnboardingRepository.countByStatusNotIn(
            List.of(OnboardingStatus.ACCOUNT_OPENED, OnboardingStatus.EXPIRED)))
        .thenReturn(40L);

    AdminDashboardSummaryResponse r = service.summary();

    assertThat(r.corporateClientCount()).isEqualTo(3L);
    assertThat(r.uploadBatchCount()).isEqualTo(11L);
    assertThat(r.correctionBatchCount()).isEqualTo(2L);
    assertThat(r.employeesInProgressCount()).isEqualTo(40L);
  }
}
