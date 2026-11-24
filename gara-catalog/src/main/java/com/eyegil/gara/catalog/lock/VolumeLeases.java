package com.eyegil.gara.catalog.lock;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.coordination.service.LeaseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Write leases per GitHub repository. GitHub turns every file write into a commit on the branch head, so two
 * writers committing to one repository at once race on that head; holding this lease keeps commits serial.
 * The lease is named after the repository, not the volume, because two buckets may share a repository.
 */
@Component
@RequiredArgsConstructor
public class VolumeLeases {

    private static final String PREFIX = "repo:";

    private final LeaseManager leaseManager;

    public Optional<VolumeLease> tryAcquire(Volume volume) {
        return leaseManager.tryAcquire(leaseName(volume))
                .map(lease -> new VolumeLease(volume, lease));
    }

    public Optional<VolumeLease> acquire(Volume volume, Duration timeout) {
        return leaseManager.acquire(leaseName(volume), timeout)
                .map(lease -> new VolumeLease(volume, lease));
    }

    public static String leaseName(Volume volume) {
        return PREFIX + volume.getRepoName();
    }
}
