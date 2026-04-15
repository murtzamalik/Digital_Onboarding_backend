package com.bank.cebos.service.auth;

import com.bank.cebos.config.MailDispatchProperties;
import com.bank.cebos.config.PasswordResetProperties;
import com.bank.cebos.entity.EmailNotificationLog;
import com.bank.cebos.repository.EmailNotificationLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DefaultPortalPasswordResetNotifier implements PortalPasswordResetNotifier {

  private static final Logger log = LoggerFactory.getLogger(DefaultPortalPasswordResetNotifier.class);

  public static final String TEMPLATE_KEY = "PORTAL_PASSWORD_RESET";
  private static final String STATUS_SENT = "SENT";
  private static final String STATUS_FAILED = "FAILED";
  private static final String STATUS_SKIPPED = "SKIPPED";

  private final PasswordResetProperties passwordResetProperties;
  private final MailDispatchProperties mailDispatchProperties;
  private final EmailNotificationLogRepository emailNotificationLogRepository;
  private final ObjectProvider<JavaMailSender> javaMailSender;
  private final ObjectMapper objectMapper;

  public DefaultPortalPasswordResetNotifier(
      PasswordResetProperties passwordResetProperties,
      MailDispatchProperties mailDispatchProperties,
      EmailNotificationLogRepository emailNotificationLogRepository,
      ObjectProvider<JavaMailSender> javaMailSender,
      ObjectMapper objectMapper) {
    this.passwordResetProperties = passwordResetProperties;
    this.mailDispatchProperties = mailDispatchProperties;
    this.emailNotificationLogRepository = emailNotificationLogRepository;
    this.javaMailSender = javaMailSender;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sendResetLink(String recipientEmail, String rawToken, String correlationId) {
    String base = passwordResetProperties.getPublicPortalBaseUrl().replaceAll("/+$", "");
    String resetUrl =
        UriComponentsBuilder.fromUriString(base)
            .path("/reset-password")
            .queryParam("token", rawToken)
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUriString();
    if (resetUrl.isBlank()) {
      persistLog(
          recipientEmail, STATUS_FAILED, correlationId, "Password reset URL could not be built");
      throw new IllegalStateException("Password reset URL could not be built");
    }

    if (!mailDispatchProperties.isDispatchEnabled()) {
      log.info(
          "Portal password reset email dispatch disabled; correlationId={} recipientEmailSuffix={}",
          correlationId,
          maskEmail(recipientEmail));
      persistLog(
          recipientEmail,
          STATUS_SKIPPED,
          correlationId,
          "SMTP dispatch disabled (cebos.mail.dispatch-enabled=false)");
      return;
    }

    JavaMailSender sender = javaMailSender.getIfAvailable();
    if (sender == null) {
      log.warn(
          "cebos.mail.dispatch-enabled=true but JavaMailSender is not available; configure spring.mail.host");
      persistLog(
          recipientEmail,
          STATUS_FAILED,
          correlationId,
          "JavaMailSender not configured (spring.mail.host not set)");
      return;
    }

    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setFrom(mailDispatchProperties.getFromAddress());
      msg.setTo(recipientEmail);
      msg.setSubject("Reset your corporate portal password");
      msg.setText(
          "You requested a password reset for the corporate onboarding portal.\n\n"
              + "Open this link to choose a new password (it expires after the time set by your administrator):\n"
              + resetUrl
              + "\n\nIf you did not request this, you can ignore this email.");
      sender.send(msg);
      persistLog(recipientEmail, STATUS_SENT, correlationId, null);
      log.info(
          "Portal password reset email sent; correlationId={} recipientEmailSuffix={}",
          correlationId,
          maskEmail(recipientEmail));
    } catch (Exception ex) {
      log.warn(
          "Portal password reset email failed; correlationId={} recipientEmailSuffix={}",
          correlationId,
          maskEmail(recipientEmail),
          ex);
      persistLog(recipientEmail, STATUS_FAILED, correlationId, ex.getMessage());
    }
  }

  private void persistLog(
      String recipientEmail, String status, String correlationId, String errorMessage) {
    try {
      String payload =
          objectMapper.writeValueAsString(
              Map.of("correlationId", correlationId != null ? correlationId : ""));
      emailNotificationLogRepository.save(
          new EmailNotificationLog(
              recipientEmail,
              TEMPLATE_KEY,
              status,
              payload,
              errorMessage,
              STATUS_SENT.equals(status) ? Instant.now() : null));
    } catch (JsonProcessingException e) {
      emailNotificationLogRepository.save(
          new EmailNotificationLog(
              recipientEmail,
              TEMPLATE_KEY,
              status,
              "{}",
              errorMessage,
              STATUS_SENT.equals(status) ? Instant.now() : null));
    }
  }

  private static String maskEmail(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      return "***";
    }
    int at = email.indexOf('@');
    if (at <= 1) {
      return "*" + email.substring(at);
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
