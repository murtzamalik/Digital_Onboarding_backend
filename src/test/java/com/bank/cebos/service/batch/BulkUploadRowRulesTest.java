package com.bank.cebos.service.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BulkUploadRowRulesTest {

  @Test
  void validRowHasNoErrors() {
    assertThat(BulkUploadRowRules.validate("11111-1111111-1", "03001234567", "Ali")).isEmpty();
  }

  @Test
  void invalidCnicReportsError() {
    assertThat(BulkUploadRowRules.validate("x", "03001234567", "Ali")).isNotEmpty();
  }

  @Test
  void normalizeCnicKeyStripsSpaces() {
    assertThat(BulkUploadRowRules.normalizeCnicKey("11111 1111111 1")).isEqualTo("1111111111111");
  }
}
