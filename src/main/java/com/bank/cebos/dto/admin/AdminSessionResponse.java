package com.bank.cebos.dto.admin;

import java.util.List;

public record AdminSessionResponse(long bankAdminUserId, List<String> authorities) {}
