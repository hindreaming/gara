package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.entity.VolumeStatus;
import com.eyegil.gara.catalog.repository.VolumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VolumeService {

    private final VolumeRepository volumeRepository;


    @Transactional
    public Volume register(String bucketId, String repoName, String directory) {
        return volumeRepository.save(new Volume(bucketId, repoName, directory));
    }

    /**
     * Counts a committed file: its bytes stay in the repository's history for good, even after a replace or delete.
     *
     * @param newFile false when the commit replaced a file already at the head
     */
    @Transactional
    public void recordWrite(Long volumeId, long bytes, boolean newFile) {
        volumeRepository.addWritten(volumeId, bytes, newFile ? 1 : 0);
    }

    /**
     * Counts a file removed from the branch head. Written bytes are untouched: history still holds them.
     */
    @Transactional
    public void recordDelete(Long volumeId) {
        volumeRepository.removeHeadFile(volumeId);
    }


    public List<Volume> findAllOrdered() {
        return volumeRepository.findAllByOrderByBucketIdAscIdAsc();
    }

    @Transactional
    public void markFull(Volume volume) {
        Volume current = get(volume.getId());
        current.setStatus(VolumeStatus.FULL);
        volumeRepository.save(current);
    }

    public Volume get(Long id) {
        return volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Volume " + id + " does not exist"));
    }

    public Volume getFirst(String bucketId) {
        return volumeRepository.findFirstByBucketIdOrderByIdAsc(bucketId)
                .orElseThrow(() -> new IllegalStateException("Bucket " + bucketId + " has no volumes"));
    }

    public List<Volume> findActive(String bucketId) {
        return volumeRepository.findAllByBucketIdAndStatus(bucketId, VolumeStatus.ACTIVE);
    }


    public long countActive(String bucketId) {
        return volumeRepository.countByBucketIdAndStatus(bucketId, VolumeStatus.ACTIVE);
    }

    public long count(String bucketId) {
        return volumeRepository.countByBucketId(bucketId);
    }

    public List<Volume> findAll() {
        return volumeRepository.findAll();
    }

    public List<Volume> findAllActive() {
        return volumeRepository.findAllByStatus(VolumeStatus.ACTIVE);
    }


    public boolean isRepoNameTaken(String repoName) {
        return volumeRepository.existsByRepoName(repoName);
    }
}
