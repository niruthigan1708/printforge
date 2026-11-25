package com.printforge.security;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {
  /** A clock the test can move forward. */
  static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
    void advance(Duration d) { now = now.plus(d); }
  }

  private final MutableClock clock = new MutableClock();
  private final LoginAttemptService service = new LoginAttemptService(3, 10, Duration.ofMinutes(15), clock);

  @Test void accountIsLockedAfterTheLimitAndUnlocksWhenTheWindowPasses() {
    for (int i = 0; i < 3; i++) { service.checkAllowed("a@b.c", "1.1.1.1"); service.recordFailure("a@b.c", "1.1.1.1"); }

    var ex = assertThrows(LoginAttemptService.TooManyAttemptsException.class, () -> service.checkAllowed("a@b.c", "2.2.2.2"));
    assertEquals(15 * 60, ex.retryAfterSeconds());

    clock.advance(Duration.ofMinutes(15));
    assertDoesNotThrow(() -> service.checkAllowed("a@b.c", "2.2.2.2"));
  }

  @Test void emailMatchingIsCaseInsensitive() {
    for (int i = 0; i < 3; i++) service.recordFailure("Case@Example.com", "1.1.1.1");
    assertThrows(LoginAttemptService.TooManyAttemptsException.class, () -> service.checkAllowed(" case@example.COM ", "9.9.9.9"));
  }

  @Test void successClearsTheAccountCount() {
    service.recordFailure("ok@b.c", "1.1.1.1");
    service.recordFailure("ok@b.c", "1.1.1.1");
    service.recordSuccess("ok@b.c");
    service.recordFailure("ok@b.c", "1.1.1.1");
    assertDoesNotThrow(() -> service.checkAllowed("ok@b.c", "1.1.1.1"));
  }

  @Test void oneIpGuessingManyAccountsIsLockedOut() {
    for (int i = 0; i < 10; i++) service.recordFailure("user" + i + "@b.c", "6.6.6.6");
    assertThrows(LoginAttemptService.TooManyAttemptsException.class, () -> service.checkAllowed("fresh@b.c", "6.6.6.6"));
    assertDoesNotThrow(() -> service.checkAllowed("fresh@b.c", "7.7.7.7"));
  }

  @Test void failuresOutsideTheWindowStartANewCount() {
    service.recordFailure("slow@b.c", "1.1.1.1");
    service.recordFailure("slow@b.c", "1.1.1.1");
    clock.advance(Duration.ofMinutes(16));
    service.recordFailure("slow@b.c", "1.1.1.1");
    assertDoesNotThrow(() -> service.checkAllowed("slow@b.c", "1.1.1.1"));
  }
}
