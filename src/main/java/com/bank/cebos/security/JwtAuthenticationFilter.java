package com.bank.cebos.security;

import com.bank.cebos.service.auth.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenService jwtTokenService;

  public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = header.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }
    String expectedAudience = resolveExpectedAudience(request.getRequestURI());
    if (expectedAudience == null) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      CebosUserDetails details = jwtTokenService.parseAndValidateAccessToken(token, expectedAudience);
      SecurityContextHolder.getContext()
          .setAuthentication(new CebosAuthenticationToken(details));
    } catch (BadCredentialsException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Unauthorized\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  static String resolveExpectedAudience(String requestUri) {
    if (requestUri == null) {
      return null;
    }
    String[] parts = requestUri.split("/");
    if (parts.length >= 4 && "api".equals(parts[1]) && "v1".equals(parts[2])) {
      String surface = parts[3];
      if ("portal".equals(surface) || "mobile".equals(surface) || "admin".equals(surface)) {
        return "/api/v1/" + surface;
      }
    }
    return null;
  }
}
