package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.config.VolumeProperty;
import com.eyegil.gara.catalog.entity.Bucket;
import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.repository.BucketRepository;
import com.eyegil.gara.catalog.util.VolumeNames;
import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.coordination.model.Lease;
import com.eyegil.gara.coordination.service.LeaseManager;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Adds new GitHub repositories to the active pool of a bucket, under a per-bucket lease so that any number of
 * instances never create two repositories for the same need.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeProvisioningService {

    private static final String LEASE_PREFIX = "provision:";

    private final VolumeProperty volumeProperty;
    private final VolumeService volumeService;
    private final GithubRepoProvisioner githubRepoProvisioner;
    private final LeaseManager leaseManager;
    private final BucketRepository bucketRepository;

    /**
     * Upload path: adds one volume if the pool still has {@code observedActive} or fewer volumes and is below
     * max-active, i.e. when every active volume is busy or none is left. A caller that lost the race simply finds
     * the volume another caller added.
     */
    public void grow(String bucketId, int observedActive) {
        try (Lease ignored = provisioningLease(bucketId)) {
            long active = volumeService.countActive(bucketId);
            if (active <= observedActive && active < volumeProperty.maxActive()) {
                provision(bucketId);
            }
        }
    }

    /**
     * Scheduler path: adds at most one volume when fewer than min-active volumes have room to spare (are below the
     * nearly-full level), without going over max-active. Reads only Postgres unless it creates a repository.
     * A bucket whose first volume is not registered yet is left alone.
     *
     * @return whether a volume was added
     */
    public boolean topUp(String bucketId) {
        if (volumeService.count(bucketId) == 0) {
            return false;
        }
        try (Lease ignored = provisioningLease(bucketId)) {
            List<Volume> active = volumeService.findActive(bucketId);
            long withRoom = active.stream().filter(this::hasRoomToSpare).count();
            if (withRoom >= volumeProperty.minActive() || active.size() >= volumeProperty.maxActive()) {
                return false;
            }
            provision(bucketId);
            return true;
        }
    }

    private boolean hasRoomToSpare(Volume volume) {
        return volume.getWrittenBytes() < volumeProperty.fillLimitBytes() * volumeProperty.nearlyFull()
                && volume.getHeadFiles() < volumeProperty.maxFiles() * volumeProperty.nearlyFull();
    }

    private Lease provisioningLease(String bucketId) {
        String name = LEASE_PREFIX + bucketId;
        return leaseManager.acquire(name, volumeProperty.leaseTimeout())
                .orElseThrow(() -> new LeaseTimeoutException(name));
    }

    private void provision(String bucketId) {
        Volume first = volumeService.getFirst(bucketId);
        String repoName = nextFreeRepoName(bucketId, first.getRepoName());
        boolean publicRepos = bucketRepository.findById(bucketId).map(Bucket::isPublicRepos).orElse(false);
        githubRepoProvisioner.create(repoName, "Gara volume for bucket " + bucketId, publicRepos);
        volumeService.register(bucketId, repoName, first.getDirectory());
        log.info("Bucket {} added volume {}", bucketId, repoName);
    }

    private String nextFreeRepoName(String bucketId, String firstRepoName) {
        int volumeNumber = (int) volumeService.count(bucketId) + 1;
        String candidate = VolumeNames.numbered(firstRepoName, volumeNumber);
        while (volumeService.isRepoNameTaken(candidate) || githubRepoProvisioner.exists(candidate)) {
            candidate = VolumeNames.numbered(firstRepoName, ++volumeNumber);
        }
        return candidate;
    }
}
