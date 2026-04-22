package com.bank.cebos.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.service.invite.InviteDispatchService;
import com.bank.cebos.util.PhoneE164;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteDispatchJobTest {

  @Mock private JobsProperties jobsProperties;
  @Mock private JobsProperties.JobSchedule inviteDispatchSchedule;
  @Mock private InviteDispatchService inviteDispatchService;

  @InjectMocks private InviteDispatchJob inviteDispatchJob;

  @Test
  void runDelegatesWhenEnabled() {
    when(jobsProperties.getInviteDispatch()).thenReturn(inviteDispatchSchedule);
    when(inviteDispatchSchedule.isEnabled()).thenReturn(true);
    when(inviteDispatchService.dispatchValidatedGlobally(
            "scheduler:invite-dispatch", "Bulk invite dispatch (validated employees)"))
        .thenReturn(3);

    inviteDispatchJob.run();

    verify(inviteDispatchService)
        .dispatchValidatedGlobally(
            "scheduler:invite-dispatch", "Bulk invite dispatch (validated employees)");
  }

  @Test
  void runSkipsWhenDisabled() {
    when(jobsProperties.getInviteDispatch()).thenReturn(inviteDispatchSchedule);
    when(inviteDispatchSchedule.isEnabled()).thenReturn(false);

    inviteDispatchJob.run();

    verifyNoInteractions(inviteDispatchService);
  }

  @Test
  void toE164NormalizesLocalPakistanMobile() {
    org.assertj.core.api.Assertions.assertThat(PhoneE164.toE164("03001234567"))
        .isEqualTo("+923001234567");
  }
}
