package com.bank.cebos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_notification_log")
public class EmailNotificationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "recipient_email", nullable = false, length = 320)
  private String recipientEmail;

  @Column(name = "template_key", nullable = false, length = 128)
  private String templateKey;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "payload_json")
  private String payloadJson;

  @Column(name = "error_message", length = 2000)
  private String errorMessage;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected EmailNotificationLog() {}

  public EmailNotificationLog(
      String recipientEmail,
      String templateKey,
      String status,
      String payloadJson,
      String errorMessage,
      Instant sentAt) {
    this.recipientEmail = recipientEmail;
    this.templateKey = templateKey;
    this.status = status;
    this.payloadJson = payloadJson;
    this.errorMessage = truncateError(errorMessage);
    this.sentAt = sentAt;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  private static String truncateError(String message) {
    if (message == null) {
      return null;
    }
    return message.length() > 2000 ? message.substring(0, 2000) : message;
  }

  public Long getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
