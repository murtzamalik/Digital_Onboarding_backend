package com.bank.cebos.repository;

import com.bank.cebos.entity.UploadBatch;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {

  Optional<UploadBatch> findByBatchReference(String batchReference);

  Optional<UploadBatch> findByBatchReferenceAndCorporateClientId(
      String batchReference, Long corporateClientId);

  Page<UploadBatch> findByCorporateClientId(Long corporateClientId, Pageable pageable);

  boolean existsByBatchReferenceAndCorporateClientId(String batchReference, Long corporateClientId);

  long countByCorporateClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long corporateClientId, Instant createdAtStart, Instant createdAtEnd);

  @Query(
      """
      SELECT COALESCE(SUM(b.totalRows), 0) FROM UploadBatch b
      WHERE b.corporateClientId = :cid
        AND b.createdAt >= :start AND b.createdAt < :end
      """)
  long sumTotalRowsByCorporateClientIdAndCreatedAtRange(
      @Param("cid") Long corporateClientId,
      @Param("start") Instant start,
      @Param("end") Instant end);
}
