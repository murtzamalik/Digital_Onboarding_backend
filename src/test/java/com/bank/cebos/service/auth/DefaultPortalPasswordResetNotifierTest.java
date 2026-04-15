package com.bank.cebos.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.cebos.config.MailDispatchProperties;
import com.bank.cebos.config.PasswordResetProperties;
import com.bank.cebos.entity.EmailNotificationLog;
import com.bank.cebos.repository.EmailNotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class DefaultPortalPasswordResetNotifierTest {

  @Mock private EmailNotificationLogRepository emailNotificationLogRepository;
  @Mock private ObjectProvider<JavaMailSender> javaMailSenderProvider;

  private DefaultPortalPasswordResetNotifier notifier;

  @BeforeEach
  void setUp() {
    PasswordResetProperties resetProps = new PasswordResetProperties();
    resetProps.setPublicPortalBaseUrl("http://localhost:5173");
    MailDispatchProperties mailProps = new MailDispatchProperties();
    mailProps.setDispatchEnabled(false);
    mailProps.setFromAddress("noreply@example.com");
    notifier =
        new DefaultPortalPasswordResetNotifier(
            resetProps,
            mailProps,
            emailNotificationLogRepository,
            javaMailSenderProvider,
            new ObjectMapper());
  }

  @Test
  void whenDispatchDisabledPersistsSkippedLog() {
    when(emailNotificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    notifier.sendResetLink("user@example.com", "deadbeef", "corr-1");

    ArgumentCaptor<EmailNotificationLog> captor = ArgumentCaptor.forClass(EmailNotificationLog.class);
    verify(emailNotificationLogRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED");
    assertThat(captor.getValue().getTemplateKey())
        .isEqualTo(DefaultPortalPasswordResetNotifier.TEMPLATE_KEY);
  }
}
