package com.bank.cebos.repository;

import com.bank.cebos.entity.BankAdminUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAdminUserRepository extends JpaRepository<BankAdminUser, Long> {

  Optional<BankAdminUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  Page<BankAdminUser> findAllByOrderByEmailAsc(Pageable pageable);
}
