package com.bank.cebos.dto.admin;

import jakarta.validation.constraints.Size;

public record UpdateSystemConfigRequest(@Size(max = 8192) String configValue) {}
