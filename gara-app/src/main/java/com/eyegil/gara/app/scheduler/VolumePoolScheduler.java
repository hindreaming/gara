package com.eyegil.gara.app.scheduler;

import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.catalog.service.VolumeProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps every bucket's pool ahead of demand: at most one new repository per bucket per run, when fewer than
 * {@code gara.volume.min-active} active repositories have room to spare. Runs once at startup, then every
 * {@code gara.volume.pool-check-interval}. The check reads only Postgres.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VolumePoolScheduler {

    private final BucketService bucketService;
    private final VolumeProvisioningService volumeProvisioningService;

    @Scheduled(fixedDelayString = "${gara.volume.pool-check-interval:PT5M}")
    public void topUpPools() {
        for (String bucketId : bucketService.names()) {
            try {
                volumeProvisioningService.topUp(bucketId);
            } catch (RuntimeException e) {
                log.error("Could not top up the volume pool of bucket {}", bucketId, e);
            }
        }
    }
}
