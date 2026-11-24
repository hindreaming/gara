package com.eyegil.gara.catalog.lock;


import com.eyegil.gara.catalog.config.VolumeProperty;
import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.coordination.model.Lease;
import com.eyegil.gara.coordination.service.LeaseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Serializes writes and deletes of the same object key across all instances, so two uploads of one key
 * cannot land in two volumes and leave the catalog pointing at the copy that gets cleaned up.
 * <p>
 * Lock order is always key first, then repository.
 */
@Component
@RequiredArgsConstructor
public class ObjectKeyLeases {

    private static final String PREFIX = "key:";

    private final LeaseManager leaseManager;
    private final VolumeProperty volumeProperty;

    public Lease acquire(String bucketId, String key) {
        String name = leaseName(bucketId, key);
        return leaseManager.acquire(name, volumeProperty.leaseTimeout())
                .orElseThrow(() -> new LeaseTimeoutException(name));
    }


    private static String leaseName(String bucketId, String key) {
        return PREFIX + bucketId + "/" + key;
    }
}
