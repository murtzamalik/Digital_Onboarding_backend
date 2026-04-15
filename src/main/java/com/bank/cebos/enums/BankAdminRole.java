package com.bank.cebos.enums;

public enum BankAdminRole {
  SUPER_ADMIN,
  OPS_MANAGER,
  OPS_STAFF,
  COMPLIANCE_OFFICER,
  VIEWER;

  public String authority() {
    return "ROLE_" + name();
  }
}
