package com.eyegil.gara.security.service;

import com.eyegil.gara.security.config.LockoutProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed logins per client address and locks an address out after too many, see {@link LockoutProperty}.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptGuard {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final LockoutProperty lockoutProperty;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLockedOut(String client) {
        Attempts current = attempts.get(client);
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant());
    }

    public void recordFailure(String client) {
        Instant now = clock.instant();
        attempts.compute(client, (key, current) -> {
            Attempts fresh = current == null || current.windowStart().plus(lockoutProperty.duration()).isBefore(now)
                    ? new Attempts(0, now, null)
                    : current;
            int failures = fresh.failures() + 1;
            Instant lockedUntil = failures >= lockoutProperty.maxFailures() ? now.plus(lockoutProperty.duration()) : null;
            return new Attempts(failures, fresh.windowStart(), lockedUntil);
        });
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(a -> a.windowStart().plus(lockoutProperty.duration().multipliedBy(2)).isBefore(now));
        }
    }

    public void recordSuccess(String client) {
        attempts.remove(client);
    }

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
    }
}
