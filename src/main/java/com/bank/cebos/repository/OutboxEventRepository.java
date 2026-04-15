package com.bank.cebos.repository;

import com.bank.cebos.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  List<OutboxEvent> findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();
}
