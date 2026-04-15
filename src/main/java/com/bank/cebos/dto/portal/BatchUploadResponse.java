package com.bank.cebos.dto.portal;

public record BatchUploadResponse(long batchId, String batchReference, int rowCount) {}
