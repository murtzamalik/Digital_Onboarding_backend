package com.bank.cebos.dto.admin;

public record AdminDashboardSummaryResponse(
    long corporateClientCount,
    long uploadBatchCount,
    long correctionBatchCount,
    long employeesInProgressCount) {

  public static AdminDashboardSummaryResponse zeros() {
    return new AdminDashboardSummaryResponse(0, 0, 0, 0);
  }
}
