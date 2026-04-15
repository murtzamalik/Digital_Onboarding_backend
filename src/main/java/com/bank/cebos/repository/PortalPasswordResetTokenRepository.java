package com.bank.cebos.repository;

import com.bank.cebos.entity.PortalPasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortalPasswordResetTokenRepository
    extends JpaRepository<PortalPasswordResetToken, Long> {

  Optional<PortalPasswordResetToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update PortalPasswordResetToken t set t.usedAt = :now "
          + "where t.corporateUserId = :userId and t.usedAt is null")
  int supersedeUnusedForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
