package com.bank.cebos.dto.mobile;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record FormSubmitRequest(@NotEmpty Map<String, Object> data) {}
