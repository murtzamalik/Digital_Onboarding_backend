package com.bank.cebos.dto.portal;

import java.util.List;

public record PortalSessionResponse(
    long portalUserId, Long corporateClientId, List<String> authorities) {}
