package com.bank.cebos.service.auth;

import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;

  public JwtTokenService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String issueAccessToken(
      PrincipalKind kind,
      long principalId,
      Long corporateClientId,
      String roles,
      String audience) {
    Instant now = Instant.now();
    Instant exp = now.plus(jwtProperties.accessTokenMinutes(), ChronoUnit.MINUTES);
    var builder =
        Jwts.builder()
            .issuer(jwtProperties.issuer())
            .subject(Long.toString(principalId))
            .audience()
            .add(audience)
            .and()
            .claim("kind", kind.name())
            .claim("roles", roles == null ? "" : roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(secretKey);
    if (corporateClientId != null) {
      builder.claim("corporateClientId", corporateClientId);
    }
    return builder.compact();
  }

  public CebosUserDetails parseAndValidateAccessToken(String token, String expectedAudience) {
    JwtParser parser =
        Jwts.parser().verifyWith(secretKey).requireIssuer(jwtProperties.issuer()).build();
    Claims claims;
    try {
      claims = parser.parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw new BadCredentialsException("Access token expired", e);
    } catch (JwtException e) {
      throw new BadCredentialsException("Invalid access token", e);
    }
    Object audClaim = claims.get("aud");
    if (!audienceContains(audClaim, expectedAudience)) {
      throw new BadCredentialsException("Invalid token audience");
    }
    PrincipalKind kind = PrincipalKind.valueOf(claims.get("kind", String.class));
    long id = Long.parseLong(claims.getSubject());
    Long corporateClientId = readLongClaim(claims, "corporateClientId");
    String roles = claims.get("roles", String.class);
    Collection<GrantedAuthority> authorities = toAuthorities(roles);
    return new CebosUserDetails(kind, id, corporateClientId, authorities);
  }

  private static boolean audienceContains(Object audClaim, String expected) {
    if (audClaim == null) {
      return false;
    }
    if (audClaim instanceof String s) {
      return expected.equals(s);
    }
    if (audClaim instanceof Collection<?> col) {
      for (Object o : col) {
        if (expected.equals(String.valueOf(o))) {
          return true;
        }
      }
    }
    return false;
  }

  private static Long readLongClaim(Claims claims, String name) {
    Object v = claims.get(name);
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    return Long.parseLong(v.toString());
  }

  private static Collection<GrantedAuthority> toAuthorities(String roles) {
    if (roles == null || roles.isBlank()) {
      return List.of();
    }
    List<GrantedAuthority> out = new ArrayList<>();
    for (String raw : roles.split(",")) {
      String r = raw.trim();
      if (r.isEmpty()) {
        continue;
      }
      if (r.startsWith("ROLE_")) {
        out.add(new SimpleGrantedAuthority(r));
      } else {
        out.add(new SimpleGrantedAuthority("ROLE_" + r));
      }
    }
    return out;
  }
}
