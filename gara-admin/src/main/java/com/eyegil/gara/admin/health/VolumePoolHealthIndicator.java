package com.eyegil.gara.admin.health;

import com.eyegil.gara.catalog.service.VolumeService;
import com.eyegil.gara.catalog.service.BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * DOWN when a configured bucket has no active volume, i.e. uploads to it would fail.
 */
@Component("volumePool")
@RequiredArgsConstructor
public class VolumePoolHealthIndicator implements HealthIndicator {

    private final BucketService bucketService;
    private final VolumeService volumeService;

    @Override
    public Health health() {
        Health.Builder health = Health.up();
        for (String bucketId : bucketService.names()) {
            long active = volumeService.countActive(bucketId);
            health.withDetail(bucketId, active + " active volume(s)");
            if (active == 0) {
                health.down();
            }
        }
        return health.build();
    }
}
