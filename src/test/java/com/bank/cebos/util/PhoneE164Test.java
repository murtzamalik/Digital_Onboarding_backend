package com.bank.cebos.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneE164Test {

  @Test
  void normalizesPakistanLocalStartingWithZero() {
    assertThat(PhoneE164.toE164("03001234567")).isEqualTo("+923001234567");
  }

  @Test
  void preservesExistingPlusPrefix() {
    assertThat(PhoneE164.toE164("+441234567890")).isEqualTo("+441234567890");
  }

  @Test
  void returnsNullForBlank() {
    assertThat(PhoneE164.toE164(null)).isNull();
    assertThat(PhoneE164.toE164("   ")).isNull();
  }
}
