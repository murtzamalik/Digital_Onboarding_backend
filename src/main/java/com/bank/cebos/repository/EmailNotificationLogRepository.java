package com.bank.cebos.repository;

import com.bank.cebos.entity.EmailNotificationLog;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {

  Page<EmailNotificationLog> findByRecipientEmailIgnoreCaseIn(
      Collection<String> recipientEmails, Pageable pageable);
}
