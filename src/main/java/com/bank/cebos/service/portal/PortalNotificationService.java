package com.bank.cebos.service.portal;

import com.bank.cebos.dto.portal.PortalNotificationItemResponse;
import com.bank.cebos.entity.EmailNotificationLog;
import com.bank.cebos.repository.CorporateUserRepository;
import com.bank.cebos.repository.EmailNotificationLogRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalNotificationService {

  private final CorporateUserRepository corporateUserRepository;
  private final EmailNotificationLogRepository emailNotificationLogRepository;

  public PortalNotificationService(
      CorporateUserRepository corporateUserRepository,
      EmailNotificationLogRepository emailNotificationLogRepository) {
    this.corporateUserRepository = corporateUserRepository;
    this.emailNotificationLogRepository = emailNotificationLogRepository;
  }

  @Transactional(readOnly = true)
  public Page<PortalNotificationItemResponse> listForClient(long corporateClientId, Pageable pageable) {
    List<String> emails = corporateUserRepository.findEmailsByCorporateClientId(corporateClientId);
    if (emails.isEmpty()) {
      return Page.empty(pageable);
    }
    return emailNotificationLogRepository
        .findByRecipientEmailIgnoreCaseIn(emails, pageable)
        .map(PortalNotificationService::toResponse);
  }

  private static PortalNotificationItemResponse toResponse(EmailNotificationLog row) {
    return new PortalNotificationItemResponse(
        row.getId(),
        row.getTemplateKey(),
        row.getStatus(),
        row.getRecipientEmail(),
        row.getCreatedAt(),
        row.getSentAt(),
        row.getErrorMessage());
  }
}
