package com.eyegil.gara.coordination;

import com.eyegil.gara.GaraIntegrationTest;
import com.eyegil.gara.coordination.model.Lease;
import com.eyegil.gara.coordination.service.PostgresLeaseManager;
import com.eyegil.gara.coordination.service.SharedRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Leases and rate budgets against a real Postgres. Each call of the lease manager is a separate owner,
 * exactly as two instances would be.
 */
class CoordinationTest extends GaraIntegrationTest {

    @Autowired
    private PostgresLeaseManager leaseManager;

    @Autowired
    private SharedRateLimiter sharedRateLimiter;

    @Test
    void aHeldLeaseExcludesOtherOwnersUntilReleased() {
        Lease first = leaseManager.tryAcquire("repo:a").orElseThrow();

        assertThat(leaseManager.tryAcquire("repo:a")).isEmpty();
        assertThat(leaseManager.acquire("repo:a", Duration.ofMillis(100))).isEmpty();
        assertThat(leaseManager.tryAcquire("repo:b")).isPresent();

        first.close();
        assertThat(leaseManager.tryAcquire("repo:a")).isPresent();
    }

    @Test
    void anExpiredLeaseCanBeTakenOverAndTheOldHolderCannotReleaseIt() {
        Lease crashed = leaseManager.tryAcquire("repo:a").orElseThrow();
        expire("repo:a");

        Lease takeover = leaseManager.tryAcquire("repo:a").orElseThrow();
        crashed.close();

        assertThat(leaseManager.tryAcquire("repo:a")).isEmpty();
        takeover.close();
    }

    @Test
    void renewalKeepsALiveHolderFromExpiring() {
        leaseManager.tryAcquire("repo:a").orElseThrow();
        Instant before = expiresAt("repo:a");

        leaseManager.renewHeldLeases();

        assertThat(expiresAt("repo:a")).isAfterOrEqualTo(before);
    }

    @Test
    void rateBudgetAllowsTheBurstThenAsksCallersToWait() {
        Duration maxWait = Duration.ofSeconds(30);

        assertThat(sharedRateLimiter.reserve("test", 3600, 2, maxWait)).contains(Duration.ZERO);
        assertThat(sharedRateLimiter.reserve("test", 3600, 2, maxWait)).contains(Duration.ZERO);
        Optional<Duration> third = sharedRateLimiter.reserve("test", 3600, 2, maxWait);

        assertThat(third).get().satisfies(wait -> assertThat(wait).isBetween(Duration.ofMillis(1), Duration.ofSeconds(1)));
        assertThat(sharedRateLimiter.reserve("test", 3600, 2, Duration.ZERO)).isEmpty();
    }

    private void expire(String name) {
        jdbcTemplate.update("UPDATE gara_lease SET expires_at = now() - INTERVAL '1 second' WHERE name = ?", name);
    }

    private Instant expiresAt(String name) {
        return jdbcTemplate.queryForObject("SELECT expires_at FROM gara_lease WHERE name = ?", Instant.class, name);
    }
}
