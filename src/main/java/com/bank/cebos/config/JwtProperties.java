package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.jwt")
public record JwtProperties(
    String issuer,
    int accessTokenMinutes,
    int refreshTokenDays,
    String secret) {}
