package com.bank.cebos.service.outbox;

import com.bank.cebos.entity.OutboxEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

@Service
public class OutboxDispatchService {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatchService.class);

  private final List<OutboxEventHandler> handlers;

  public OutboxDispatchService(List<OutboxEventHandler> handlers) {
    this.handlers =
        handlers.stream()
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .toList();
  }

  public void dispatch(OutboxEvent event) throws Exception {
    for (OutboxEventHandler handler : handlers) {
      if (handler.supports(event.getEventType())) {
        handler.handle(event);
        return;
      }
    }
    log.info(
        "Outbox event id={} type={} has no handler; acknowledged without side effects",
        event.getId(),
        event.getEventType());
  }
}
