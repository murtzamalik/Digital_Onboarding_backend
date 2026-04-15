package com.bank.cebos.service.outbox;

import com.bank.cebos.entity.OutboxEvent;

public interface OutboxEventHandler {

  boolean supports(String eventType);

  void handle(OutboxEvent event) throws Exception;
}
