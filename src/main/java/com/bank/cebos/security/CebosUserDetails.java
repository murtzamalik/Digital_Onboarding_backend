package com.bank.cebos.security;

import com.bank.cebos.enums.PrincipalKind;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record CebosUserDetails(
    PrincipalKind kind,
    long id,
    Long corporateClientId,
    Collection<? extends GrantedAuthority> authorities) {}
