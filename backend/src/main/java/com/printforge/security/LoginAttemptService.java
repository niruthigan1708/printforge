package com.printforge.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles password guessing. Failed logins are counted per account (email) and per client IP
 * within a sliding window; once either limit is reached, further attempts are refused until the
 * window since the first counted failure has passed. A successful login clears the account's count.
 *
 * <p>State is in memory, so it resets on restart and is per instance — enough for a single-node
 * deployment. Behind a reverse proxy, configure the proxy to pass the real client IP (Spring's
 * {@code server.forward-headers-strategy}) or the IP limit applies to the proxy as a whole.
 */
@Service
public class LoginAttemptService {
  private final int maxFailuresPerAccount;
  private final int maxFailuresPerIp;
  private final Duration window;
  private final Clock clock;
  private final Map<String, Attempts> failures = new ConcurrentHashMap<>();

  private record Attempts(int count, Instant windowStart) { }

  /** Thrown when a login is refused because of too many recent failures. */
  public static class TooManyAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;
    public TooManyAttemptsException(long retryAfterSeconds) {
      super("Too many failed login attempts. Please try again in " + Math.max(1, (retryAfterSeconds + 59) / 60) + " minute(s).");
      this.retryAfterSeconds = retryAfterSeconds;
    }
    public long retryAfterSeconds() { return retryAfterSeconds; }
  }

  @Autowired
  public LoginAttemptService(@Value("${app.auth.max-failed-logins-per-account:5}") int maxFailuresPerAccount,
                             @Value("${app.auth.max-failed-logins-per-ip:20}") int maxFailuresPerIp,
                             @Value("${app.auth.lockout-window:15m}") Duration window) {
    this(maxFailuresPerAccount, maxFailuresPerIp, window, Clock.systemUTC());
  }

  LoginAttemptService(int maxFailuresPerAccount, int maxFailuresPerIp, Duration window, Clock clock) {
    this.maxFailuresPerAccount = maxFailuresPerAccount;
    this.maxFailuresPerIp = maxFailuresPerIp;
    this.window = window;
    this.clock = clock;
  }

  /** Throws if this account or IP is currently locked out. Call before checking the password. */
  public void checkAllowed(String email, String ip) {
    Instant now = clock.instant();
    long wait = Math.max(remainingLockout(accountKey(email), maxFailuresPerAccount, now), remainingLockout(ipKey(ip), maxFailuresPerIp, now));
    if (wait > 0) throw new TooManyAttemptsException(wait);
  }

  public void recordFailure(String email, String ip) {
    Instant now = clock.instant();
    increment(accountKey(email), now);
    increment(ipKey(ip), now);
    if (failures.size() > 10_000) prune(now); // keep memory bounded under a spray of random emails
  }

  /** A correct password clears the account's failures (not the IP's, so one known account can't reset guessing). */
  public void recordSuccess(String email) { failures.remove(accountKey(email)); }

  private long remainingLockout(String key, int limit, Instant now) {
    Attempts attempts = failures.get(key);
    if (attempts == null || attempts.count() < limit) return 0;
    long remaining = Duration.between(now, attempts.windowStart().plus(window)).getSeconds();
    if (remaining <= 0) { failures.remove(key, attempts); return 0; }
    return remaining;
  }

  private void increment(String key, Instant now) {
    failures.compute(key, (k, current) -> current == null || !now.isBefore(current.windowStart().plus(window))
      ? new Attempts(1, now)
      : new Attempts(current.count() + 1, current.windowStart()));
  }

  private void prune(Instant now) {
    failures.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().windowStart().plus(window)));
  }

  private static String accountKey(String email) { return "account:" + (email == null ? "" : email.trim().toLowerCase()); }
  private static String ipKey(String ip) { return "ip:" + (ip == null ? "unknown" : ip); }
}
