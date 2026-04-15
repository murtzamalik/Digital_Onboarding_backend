package com.bank.cebos.service.auth;

/**
 * Delivers (or enqueues) corporate portal password-reset links. Raw token must only be passed to the
 * user channel (email/SMS adapter), never persisted beyond hashed storage.
 */
public interface PortalPasswordResetNotifier {

  void sendResetLink(String recipientEmail, String rawToken, String correlationId);
}
