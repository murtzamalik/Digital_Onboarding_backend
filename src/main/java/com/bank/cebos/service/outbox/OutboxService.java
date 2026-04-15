package com.bank.cebos.service.outbox;

import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

  private final OutboxEventRepository outboxEventRepository;

  public OutboxService(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  @Transactional
  public void enqueue(
      String eventType, String aggregateType, String aggregateId, String payloadJson) {
    OutboxEvent row = new OutboxEvent();
    row.setEventType(eventType);
    row.setAggregateType(aggregateType);
    row.setAggregateId(aggregateId);
    row.setPayloadJson(payloadJson);
    outboxEventRepository.save(row);
  }
}
