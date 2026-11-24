package com.eyegil.gara.coordination.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Leases coordinate Gara instances through Postgres.
 * {@code renew-interval} is also read directly by the renewal scheduler, so keep it in ISO format (PT30S).
 *
 * @param ttl           how long a lease survives without renewal; a crashed instance's leases free up after this
 * @param renewInterval how often a live instance extends the leases it holds; must be well below {@code ttl}
 * @param pollInterval  how often a waiting caller retries a lease that is taken
 */
@ConfigurationProperties("gara.lease")
public record LeaseProperty(
        @DefaultValue("2m") Duration ttl,
        @DefaultValue("PT30S") Duration renewInterval,
        @DefaultValue("100ms") Duration pollInterval
) {

    public LeaseProperty {
        if (renewInterval.compareTo(ttl.dividedBy(2)) > 0) {
            throw new IllegalArgumentException("gara.lease.renew-interval must be at most half of gara.lease.ttl");
        }
    }
}
