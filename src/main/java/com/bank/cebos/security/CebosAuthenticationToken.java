package com.bank.cebos.security;

import java.util.ArrayList;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class CebosAuthenticationToken extends AbstractAuthenticationToken {

  private final CebosUserDetails principal;

  public CebosAuthenticationToken(CebosUserDetails principal) {
    super(new ArrayList<>(principal.authorities()));
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public CebosUserDetails getPrincipal() {
    return principal;
  }
}
