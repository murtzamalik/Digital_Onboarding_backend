package com.bank.cebos.repository;

import com.bank.cebos.dto.admin.AdminStatusHistoryResponse;
import com.bank.cebos.entity.EmployeeStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeStatusHistoryRepository extends JpaRepository<EmployeeStatusHistory, Long> {

  @Query(
      """
      SELECT new com.bank.cebos.dto.admin.AdminStatusHistoryResponse(
          h.id, e.employeeRef, h.fromStatus, h.toStatus, h.changedBy, h.reason, h.createdAt)
      FROM EmployeeStatusHistory h
      JOIN h.employeeOnboarding e
      ORDER BY h.createdAt DESC
      """)
  Page<AdminStatusHistoryResponse> findRecentForAudit(Pageable pageable);
}
