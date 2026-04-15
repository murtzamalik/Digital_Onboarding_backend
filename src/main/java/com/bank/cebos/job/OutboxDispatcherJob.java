package com.bank.cebos.job;

import com.bank.cebos.config.OutboxProperties;
import com.bank.cebos.entity.OutboxEvent;
import com.bank.cebos.repository.OutboxEventRepository;
import com.bank.cebos.service.outbox.OutboxDispatchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls unprocessed outbox rows, dispatches via {@link OutboxDispatchService}, and sets {@code
 * processed_at} only after successful handling so failures can be retried.
 */
@Component
public class OutboxDispatcherJob {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcherJob.class);

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxProperties outboxProperties;
  private final OutboxDispatchService outboxDispatchService;

  public OutboxDispatcherJob(
      OutboxEventRepository outboxEventRepository,
      OutboxProperties outboxProperties,
      OutboxDispatchService outboxDispatchService) {
    this.outboxEventRepository = outboxEventRepository;
    this.outboxProperties = outboxProperties;
    this.outboxDispatchService = outboxDispatchService;
  }

  @Scheduled(fixedDelayString = "${cebos.outbox.poll-interval-ms:5000}")
  @Transactional
  public void run() {
    if (!outboxProperties.isEnabled()) {
      return;
    }

    List<OutboxEvent> batch =
        outboxEventRepository.findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();
    if (batch.isEmpty()) {
      return;
    }

    Instant now = Instant.now();
    List<OutboxEvent> toSave = new ArrayList<>();
    for (OutboxEvent event : batch) {
      try {
        outboxDispatchService.dispatch(event);
        event.setProcessedAt(now);
        toSave.add(event);
      } catch (Exception e) {
        log.warn(
            "Outbox event id={} type={} dispatch failed; will retry",
            event.getId(),
            event.getEventType(),
            e);
      }
    }
    if (!toSave.isEmpty()) {
      outboxEventRepository.saveAll(toSave);
      log.info("OutboxDispatcherJob completed {} of {} row(s)", toSave.size(), batch.size());
    }
  }
}
