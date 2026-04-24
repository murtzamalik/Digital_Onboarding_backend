package com.bank.cebos.logging;

import com.bank.cebos.config.HttpLoggingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Emits a single INFO line so operators can confirm logging and HTTP capture flags at startup. */
@Component
@Order(0)
public class StartupLoggingRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupLoggingRunner.class);

  private final HttpLoggingProperties httpLoggingProperties;

  public StartupLoggingRunner(HttpLoggingProperties httpLoggingProperties) {
    this.httpLoggingProperties = httpLoggingProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info(
        "CEBOS API ready. HttpExchangeLoggingFilter enabled={}. "
            + "Each request logs one INFO line; full bodies/headers at DEBUG for "
            + "logger com.bank.cebos.logging. BBS: log-bbs-http={}; full payloads at DEBUG for "
            + "logger com.bank.cebos.integration.bbs.",
        httpLoggingProperties.isEnabled(),
        httpLoggingProperties.isLogBbsHttp());
  }
}
