package com.eyegil.gara.coordination.service;


import com.eyegil.gara.coordination.config.LeaseProperty;
import com.eyegil.gara.coordination.dto.ActiveLease;
import com.eyegil.gara.coordination.model.Lease;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leases as rows of {@code gara_lease}. Taking a lease is a single upsert that only wins when the row is
 * missing or expired; holders renew their rows in the background, so only a crashed instance's leases expire.
 */
@Slf4j
@Service
public class PostgresLeaseManager implements LeaseManager {

    private static final String ACQUIRE = """
            INSERT INTO gara_lease (name, owner_id, expires_at)
            VALUES (?, ?, now() + ? * INTERVAL '1 millisecond')
            ON CONFLICT (name) DO UPDATE
                SET owner_id = EXCLUDED.owner_id, expires_at = EXCLUDED.expires_at
                WHERE gara_lease.expires_at < now()
            """;
    private static final String RENEW = """
            UPDATE gara_lease SET expires_at = now() + ? * INTERVAL '1 millisecond'
            WHERE name = ? AND owner_id = ?
            """;
    private static final String RELEASE = "DELETE FROM gara_lease WHERE name = ? AND owner_id = ?";
    private static final String ACTIVE = "SELECT name, owner_id, expires_at FROM gara_lease WHERE expires_at >= now() ORDER BY name";

    private final JdbcTemplate jdbcTemplate;
    private final LeaseProperty leaseProperty;
    private final String instanceId = instanceName() + "/" + UUID.randomUUID();
    private final Map<String, Lease> held = new ConcurrentHashMap<>();

    public PostgresLeaseManager(JdbcTemplate jdbcTemplate, LeaseProperty leaseProperty) {
        this.jdbcTemplate = jdbcTemplate;
        this.leaseProperty = leaseProperty;
    }

    @Override
    public Optional<Lease> tryAcquire(String name) {
        String ownerId = instanceId + "#" + UUID.randomUUID();
        if (jdbcTemplate.update(ACQUIRE, name, ownerId, leaseProperty.ttl().toMillis()) == 0) {
            return Optional.empty();
        }
        Lease lease = new Lease(name, ownerId, this::release);
        held.put(ownerId, lease);
        return Optional.of(lease);
    }

    @Override
    public Optional<Lease> acquire(String name, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            Optional<Lease> lease = tryAcquire(name);
            if (lease.isPresent() || System.nanoTime() >= deadline) {
                return lease;
            }
            try {
                Thread.sleep(leaseProperty.pollInterval().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    @Override
    public List<ActiveLease> active() {
        return jdbcTemplate.query(ACTIVE, (rs, row) -> new ActiveLease(
                rs.getString("name"), rs.getString("owner_id"), rs.getTimestamp("expires_at").toInstant()));
    }

    @Scheduled(fixedDelayString = "${gara.lease.renew-interval:PT30S}")
    public void renewHeldLeases() {
        for (Lease lease : held.values()) {
            int renewed = jdbcTemplate.update(RENEW, leaseProperty.ttl().toMillis(), lease.name(), lease.ownerId());
            if (renewed == 0) {
                held.remove(lease.ownerId());
                log.error("Lease {} expired before it could be renewed; another holder may have taken it", lease.name());
            }
        }
    }

    /**
     * Frees this instance's leases on shutdown instead of leaving them to expire.
     */
    @PreDestroy
    public void releaseAll() {
        held.values().forEach(this::release);
    }

    private void release(Lease lease) {
        held.remove(lease.ownerId());
        jdbcTemplate.update(RELEASE, lease.name(), lease.ownerId());
    }

    private static String instanceName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}
