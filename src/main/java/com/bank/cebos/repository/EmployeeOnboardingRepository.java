package com.bank.cebos.repository;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.bank.cebos.enums.OnboardingStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeOnboardingRepository extends JpaRepository<EmployeeOnboarding, Long> {

  Optional<EmployeeOnboarding> findByEmployeeRef(String employeeRef);

  Optional<EmployeeOnboarding> findByMobileAndStatus(String mobile, OnboardingStatus status);

  List<EmployeeOnboarding> findByBatchId(Long batchId);

  List<EmployeeOnboarding> findByBatchIdAndStatus(Long batchId, OnboardingStatus status);

  long countByBatchId(Long batchId);

  long countByBatchIdAndStatus(Long batchId, OnboardingStatus status);

  long countByStatusNotIn(Collection<OnboardingStatus> statuses);

  @Query(
      """
      SELECT e FROM EmployeeOnboarding e
      WHERE e.batchId = (
        SELECT b.id FROM UploadBatch b WHERE b.batchReference = :batchReference
      )
      """)
  List<EmployeeOnboarding> findByBatchReference(@Param("batchReference") String batchReference);

  List<EmployeeOnboarding> findByStatusInAndExpireAtBefore(
      Collection<OnboardingStatus> statuses, Instant cutoff);

  List<EmployeeOnboarding> findByStatus(OnboardingStatus status, Pageable pageable);

  Page<EmployeeOnboarding> findByCorporateClientIdAndStatus(
      Long corporateClientId, OnboardingStatus status, Pageable pageable);

  @Query(
      """
      SELECT e
      FROM EmployeeOnboarding e
      WHERE e.status = :status
        AND (
          (e.inviteSentAt IS NOT NULL AND e.inviteSentAt < :cutoff)
          OR (e.inviteSentAt IS NULL AND e.createdAt < :cutoff)
        )
      """)
  List<EmployeeOnboarding> findInvitedOlderThan(
      @Param("status") OnboardingStatus status, @Param("cutoff") Instant cutoff);

  @Query(
      """
      SELECT e FROM EmployeeOnboarding e
      WHERE (:status IS NULL OR e.status = :status)
      AND (:q IS NULL OR :q = ''
          OR LOWER(e.employeeRef) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
      """)
  Page<EmployeeOnboarding> adminSearch(
      @Param("status") OnboardingStatus status, @Param("q") String q, Pageable pageable);
}
