package com.bank.cebos.enums;

public enum PortalRole {
  ADMIN,
  VIEWER;

  public String authority() {
    return "ROLE_" + name();
  }
}
