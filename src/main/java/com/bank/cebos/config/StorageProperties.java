package com.bank.cebos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cebos.storage")
public record StorageProperties(String uploadDir) {}
