package com.eyegil.gara.s3.service;

import com.eyegil.gara.catalog.dto.StoredFile;
import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.catalog.lock.ObjectKeyLeases;
import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.catalog.service.ObjectFileStore;
import com.eyegil.gara.coordination.model.Lease;
import com.eyegil.gara.s3.converter.S3ObjectConverter;
import com.eyegil.gara.s3.dto.S3ObjectMetadata;
import com.eyegil.gara.s3.dto.S3StoredObject;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.s3.util.ETags;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Object operations. Each key is one file at the key's path in one repository; the catalog in Postgres says
 * which repository, so reads, HEAD and LIST never search GitHub. Writes and deletes of one key are serialized
 * across instances by a key lease.
 */
@Service
@RequiredArgsConstructor
public class S3ObjectService {

    private final UploadLimits uploadLimits;
    private final S3BucketService s3BucketService;
    private final ObjectCatalogService objectCatalogService;
    private final ObjectKeyLeases objectKeyLeases;
    private final ObjectFileStore objectFileStore;
    private final S3ObjectConverter s3ObjectConverter;

    public S3ObjectMetadata put(String bucketId, String key, byte[] content, String contentType) {
        s3BucketService.requireExists(bucketId);
        uploadLimits.requireStorableSize(content);
        return store(bucketId, key, content, ETags.of(content), contentType);
    }

    /**
     * Writes {@code content} as the file of {@code key}: in place if a repository already holds the key (a full one
     * included), otherwise in a free active repository.
     */
    public S3ObjectMetadata store(String bucketId, String key, byte[] content, String etag, String contentType) {
        String type = StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try (Lease ignored = objectKeyLeases.acquire(bucketId, key)) {
            Optional<StoredObject> existing = objectCatalogService.find(bucketId, key);
            StoredFile file = existing.isPresent()
                    ? objectFileStore.overwrite(existing.get().getVolumeId(), key, content, existing.get().getGitSha())
                    : objectFileStore.writeNew(bucketId, key, content);
            return s3ObjectConverter.toMetadata(
                    objectCatalogService.save(file.volume(), key, content.length, etag, type, file.gitSha()));
        }
    }

    /**
     * Copies an object to another key (possibly in another bucket). The target keeps the source's content type
     * unless {@code replacementContentType} is given (metadata directive REPLACE).
     */
    public S3ObjectMetadata copy(String sourceBucketId, String sourceKey, String bucketId, String key,
                                 String replacementContentType) {
        s3BucketService.requireExists(bucketId);
        S3StoredObject source = get(sourceBucketId, sourceKey);
        String type = replacementContentType != null ? replacementContentType : source.metadata().contentType();
        return store(bucketId, key, source.content(), ETags.of(source.content()), type);
    }

    public S3ObjectMetadata head(String bucketId, String key) {
        return s3ObjectConverter.toMetadata(locate(bucketId, key));
    }

    public S3StoredObject get(String bucketId, String key) {
        StoredObject object = locate(bucketId, key);
        byte[] content = objectFileStore.read(object.getVolumeId(), object.getGitSha());
        return new S3StoredObject(s3ObjectConverter.toMetadata(object), content);
    }

    /**
     * Deletes the object if it exists. Deleting a missing key succeeds, as in S3.
     */
    public void delete(String bucketId, String key) {
        s3BucketService.requireExists(bucketId);
        try (Lease ignored = objectKeyLeases.acquire(bucketId, key)) {
            objectCatalogService.find(bucketId, key).ifPresent(object -> {
                objectFileStore.delete(object.getVolumeId(), key, object.getGitSha());
                objectCatalogService.remove(object);
            });
        }
    }

    private StoredObject locate(String bucketId, String key) {
        s3BucketService.requireExists(bucketId);
        return objectCatalogService.find(bucketId, key)
                .orElseThrow(() -> new S3Exception(S3ErrorCode.NO_SUCH_KEY));
    }
}
