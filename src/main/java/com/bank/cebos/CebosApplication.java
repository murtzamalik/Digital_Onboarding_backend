package com.bank.cebos;

import com.bank.cebos.config.BbsKycProperties;
import com.bank.cebos.config.CorsProperties;
import com.bank.cebos.config.JobsProperties;
import com.bank.cebos.config.JwtProperties;
import com.bank.cebos.config.MailDispatchProperties;
import com.bank.cebos.config.OtpProperties;
import com.bank.cebos.config.OutboxProperties;
import com.bank.cebos.config.PasswordResetProperties;
import com.bank.cebos.config.RateLimitProperties;
import com.bank.cebos.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
  BbsKycProperties.class,
  CorsProperties.class,
  JwtProperties.class,
  JobsProperties.class,
  MailDispatchProperties.class,
  OtpProperties.class,
  OutboxProperties.class,
  PasswordResetProperties.class,
  RateLimitProperties.class,
  StorageProperties.class
})
public class CebosApplication {

  public static void main(String[] args) {
    SpringApplication.run(CebosApplication.class, args);
  }
}
