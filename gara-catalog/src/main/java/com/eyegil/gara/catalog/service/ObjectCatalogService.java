package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.repository.StoredObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Which repository holds each object key, and the metadata S3 clients read.
 */
@Service
@RequiredArgsConstructor
public class ObjectCatalogService {

    private final StoredObjectRepository storedObjectRepository;

    public Optional<StoredObject> find(String bucketId, String objectKey) {
        return storedObjectRepository.findByBucketIdAndObjectKey(bucketId, objectKey);
    }

    /**
     * Up to {@code limit} objects of a bucket whose keys start with {@code prefix} and sort after {@code startAfter},
     * in byte order of the key.
     */
    public List<StoredObject> list(String bucketId, String prefix, String startAfter, int limit) {
        return storedObjectRepository.findByBucketIdAndObjectKeyStartingWithAndObjectKeyGreaterThanOrderByObjectKeyAsc(
                bucketId, prefix, startAfter, PageRequest.of(0, limit));
    }

    /**
     * Records that {@code volume} now holds {@code objectKey} with this content.
     */
    @Transactional
    public StoredObject save(Volume volume, String objectKey, long size, String etag, String contentType, String gitSha) {
        StoredObject object = find(volume.getBucketId(), objectKey)
                .orElseGet(() -> new StoredObject(volume.getBucketId(), objectKey));
        object.setVolumeId(volume.getId());
        object.setSize(size);
        object.setEtag(etag);
        object.setContentType(contentType);
        object.setGitSha(gitSha);
        return storedObjectRepository.save(object);
    }


    @Transactional
    public void remove(StoredObject object) {
        storedObjectRepository.delete(object);
    }

    public long count(String bucketId) {
        return storedObjectRepository.countByBucketId(bucketId);
    }

    public long totalSize(String bucketId) {
        return storedObjectRepository.sumSizeByBucketId(bucketId);
    }

    public long count(String bucketId, String prefix) {
        return storedObjectRepository.countByBucketIdAndObjectKeyStartingWith(bucketId, prefix);
    }

    /**
     * @param prefix a literal key prefix without the LIKE wildcards {@code %} and {@code _}
     */
    public long totalSize(String bucketId, String prefix) {
        return storedObjectRepository.sumSizeByBucketIdAndPrefix(bucketId, prefix);
    }
}
