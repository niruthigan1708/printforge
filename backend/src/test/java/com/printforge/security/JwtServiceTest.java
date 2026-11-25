package com.printforge.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
  @Test void missingSecretFailsStartup() {
    assertThrows(IllegalStateException.class, () -> new JwtService("", 1000));
    assertThrows(IllegalStateException.class, () -> new JwtService(null, 1000));
  }

  @Test void placeholderSecretFromEnvExampleIsRejected() {
    assertThrows(IllegalStateException.class, () -> new JwtService(JwtService.EXAMPLE_SECRET, 1000));
  }

  @Test void shortSecretIsRejected() {
    assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 1000));
  }

  @Test void strongSecretIsAccepted() {
    assertDoesNotThrow(() -> new JwtService("a-perfectly-long-random-test-secret-value", 1000));
  }
}
