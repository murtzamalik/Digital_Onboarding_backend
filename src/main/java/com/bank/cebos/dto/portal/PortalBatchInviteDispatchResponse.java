package com.bank.cebos.dto.portal;

/** Result of manually dispatching VALIDATED → INVITED for one upload batch (portal ADMIN). */
public record PortalBatchInviteDispatchResponse(
    int attempted, int transitioned, int smsEnqueued, int transitionErrors) {}
