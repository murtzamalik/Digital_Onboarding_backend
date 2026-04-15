package com.bank.cebos.repository;

import com.bank.cebos.entity.CorporateUser;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorporateUserRepository extends JpaRepository<CorporateUser, Long> {

  List<CorporateUser> findByEmailIgnoreCase(String email);

  Page<CorporateUser> findByCorporateClientId(Long corporateClientId, Pageable pageable);

  boolean existsByCorporateClientIdAndEmailIgnoreCase(Long corporateClientId, String email);

  @Query("select lower(u.email) from CorporateUser u where u.corporateClientId = :clientId")
  List<String> findEmailsByCorporateClientId(@Param("clientId") Long clientId);
}
