package com.bank.cebos.repository;

import com.bank.cebos.entity.OtpSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {

  Optional<OtpSession> findTopByEmployeeOnboardingIdOrderByCreatedAtDesc(Long employeeOnboardingId);

  long countByEmployeeOnboardingIdAndCreatedAtAfter(Long employeeOnboardingId, Instant createdAtAfter);

  @Modifying
  @Query(
      value =
          "UPDATE otp_sessions SET status = 'EXPIRED' "
              + "WHERE status = 'ACTIVE' AND expires_at < :cutoff",
      nativeQuery = true)
  int expireActiveSessions(@Param("cutoff") Instant cutoff);
}
