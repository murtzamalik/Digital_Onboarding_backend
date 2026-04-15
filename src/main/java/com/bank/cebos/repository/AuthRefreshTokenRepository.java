package com.bank.cebos.repository;

import com.bank.cebos.entity.AuthRefreshToken;
import com.bank.cebos.enums.PrincipalKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

  Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update AuthRefreshToken t set t.revoked = true "
          + "where t.principalKind = :kind and t.principalId = :principalId and t.revoked = false")
  int revokeAllActiveForPrincipal(
      @Param("kind") PrincipalKind kind, @Param("principalId") long principalId);
}
