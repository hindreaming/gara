package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.config.VolumeProperty;
import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.exception.VolumePoolBusyException;
import com.eyegil.gara.catalog.lock.VolumeLease;
import com.eyegil.gara.catalog.lock.VolumeLeases;
import com.eyegil.gara.coordination.config.LeaseProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Hands out write leases on the active volumes of a bucket. Leases live in Postgres, so a volume that is
 * busy on one instance is busy for all of them.
 * <ol>
 *     <li>Take an idle active volume, least written first.</li>
 *     <li>If every active volume is busy and the pool is below max-active, create a new volume.</li>
 *     <li>Otherwise poll until one frees up or the lease timeout passes.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class VolumePoolService {

    private final VolumeProperty volumeProperty;
    private final LeaseProperty leaseProperty;
    private final VolumeService volumeService;
    private final VolumeLeases volumeLeases;
    private final VolumeProvisioningService volumeProvisioningService;

    public VolumeLease acquire(String bucketId) {
        long deadline = System.nanoTime() + volumeProperty.leaseTimeout().toNanos();

        while (true) {
            List<Volume> active = volumeService.findActive(bucketId).stream()
                    .sorted(Comparator.comparingLong(Volume::getWrittenBytes))
                    .toList();
            for (Volume volume : active) {
                Optional<VolumeLease> lease = volumeLeases.tryAcquire(volume);
                if (lease.isPresent()) {
                    return lease.get();
                }
            }
            if (active.size() < volumeProperty.maxActive()) {
                volumeProvisioningService.grow(bucketId, active.size());
            } else {
                pause(deadline, bucketId);
            }
        }
    }

    /**
     * Leases a specific volume, for writes that must hit the volume already holding a key (replace, delete),
     * even when that volume is full.
     */
    public VolumeLease lock(Volume volume) {
        return volumeLeases.acquire(volume, volumeProperty.leaseTimeout())
                .orElseThrow(() -> new VolumePoolBusyException(volume.getBucketId()));
    }

    private void pause(long deadline, String bucketId) {
        if (System.nanoTime() >= deadline) {
            throw new VolumePoolBusyException(bucketId);
        }
        try {
            Thread.sleep(leaseProperty.pollInterval().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VolumePoolBusyException(bucketId);
        }
    }
}
