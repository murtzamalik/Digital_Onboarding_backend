package com.bank.cebos.repository;

import com.bank.cebos.entity.SystemConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {

  Optional<SystemConfiguration> findByConfigKey(String configKey);
}
