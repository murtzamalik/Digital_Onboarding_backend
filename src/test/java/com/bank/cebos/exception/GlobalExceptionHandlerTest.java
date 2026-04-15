package com.bank.cebos.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.security.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void responseStatusExceptionBodyIncludesCorrelationIdFromMdc() throws Exception {
    MDC.put(CorrelationIdFilter.MDC_KEY, "corr-integration-test");

    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    String json =
        mockMvc
            .perform(get("/__test/throw-status"))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(json).contains("conflict-reason").contains("correlationId").contains("corr-integration-test");
  }

  @RestController
  static class ThrowingController {
    @GetMapping("/__test/throw-status")
    void throwStatus() {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "conflict-reason");
    }
  }
}
