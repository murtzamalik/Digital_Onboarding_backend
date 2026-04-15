package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.otp")
public record OtpProperties(
    int length,
    int expiryMinutes,
    int maxAttempts,
    int maxResends,
    int resendWindowMinutes) {}
