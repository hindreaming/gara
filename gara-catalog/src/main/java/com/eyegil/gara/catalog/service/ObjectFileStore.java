package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.dto.StoredFile;
import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.lock.VolumeLease;
import com.eyegil.gara.catalog.util.VolumeNames;
import com.eyegil.gara.github.service.GithubBlobReader;
import com.eyegil.gara.github.service.GithubFileWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Object files in a bucket's volumes, one file per key at {@code directory/key}. Every write holds the
 * repository's lease, so commits to one repository never race. Size accounting follows git: a write adds to the
 * volume's written bytes for good; a delete only lowers the number of files at the head.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectFileStore {

    private final VolumePoolService volumePoolService;
    private final VolumeService volumeService;
    private final VolumeCapacityService volumeCapacityService;
    private final GithubFileWriter githubFileWriter;
    private final GithubBlobReader githubBlobReader;

    /**
     * Stores a key that no volume holds yet, in any free active volume of the bucket.
     */
    public StoredFile writeNew(String bucketId, String key, byte[] content) {
        StoredFile stored;
        try (VolumeLease lease = volumePoolService.acquire(bucketId)) {
            stored = commit(lease.volume(), key, content, null);
            volumeService.recordWrite(lease.volume().getId(), content.length, true);
        }
        retireIfFull(stored.volume());
        return stored;
    }

    /**
     * Replaces the file of a key in the volume that already holds it, even when that volume is full,
     * so every key keeps exactly one file.
     */
    public StoredFile overwrite(Long volumeId, String key, byte[] content, String currentSha) {
        StoredFile stored;
        try (VolumeLease lease = volumePoolService.lock(volumeService.get(volumeId))) {
            stored = commit(lease.volume(), key, content, currentSha);
            volumeService.recordWrite(volumeId, content.length, false);
        }
        retireIfFull(stored.volume());
        return stored;
    }

    public byte[] read(Long volumeId, String gitSha) {
        return githubBlobReader.read(volumeService.get(volumeId).getRepoName(), gitSha);
    }

    public void delete(Long volumeId, String key, String currentSha) {
        try (VolumeLease lease = volumePoolService.lock(volumeService.get(volumeId))) {
            Volume volume = lease.volume();
            githubFileWriter.delete(volume.getRepoName(), VolumeNames.repoPath(volume.getDirectory(), key), currentSha);
            volumeService.recordDelete(volumeId);
        }
    }

    private StoredFile commit(Volume volume, String key, byte[] content, String currentSha) {
        String path = VolumeNames.repoPath(volume.getDirectory(), key);
        return new StoredFile(volume, githubFileWriter.put(volume.getRepoName(), path, content, currentSha));
    }

    private void retireIfFull(Volume volume) {
        try {
            volumeCapacityService.retireIfFull(volume);
        } catch (RuntimeException e) {
            log.error("Capacity check after write failed for volume {}", volume.getRepoName(), e);
        }
    }
}
