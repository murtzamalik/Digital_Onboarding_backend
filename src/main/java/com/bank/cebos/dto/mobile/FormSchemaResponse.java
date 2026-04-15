package com.bank.cebos.dto.mobile;

import java.util.List;

public record FormSchemaResponse(String templateId, List<FormFieldResponse> fields) {}
