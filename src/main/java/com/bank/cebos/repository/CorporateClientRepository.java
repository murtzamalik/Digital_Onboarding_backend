package com.bank.cebos.repository;

import com.bank.cebos.entity.CorporateClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorporateClientRepository extends JpaRepository<CorporateClient, Long> {

  boolean existsByClientCodeIgnoreCase(String clientCode);
}
