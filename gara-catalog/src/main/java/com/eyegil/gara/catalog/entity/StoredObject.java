package com.eyegil.gara.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An object key of a bucket: the volume whose repository holds it as one file at the key's path, and the
 * metadata S3 clients read, so HEAD and LIST never call GitHub.
 */
@Entity
@Table(name = "gara_object")
@Getter
@Setter
@NoArgsConstructor
public class StoredObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_id", nullable = false)
    private String bucketId;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "volume_id", nullable = false)
    private Long volumeId;

    @Column(name = "size", nullable = false)
    private long size;

    /**
     * Unquoted S3 entity tag: MD5 hex for a plain upload, {@code md5-of-part-md5s-N} for a multipart upload.
     */
    @Column(name = "etag", nullable = false, length = 64)
    private String etag;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * Git blob sha of the file, which GitHub requires to overwrite or delete it.
     */
    @Column(name = "git_sha", nullable = false, length = 40)
    private String gitSha;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StoredObject(String bucketId, String objectKey) {
        this.bucketId = bucketId;
        this.objectKey = objectKey;
    }

    @PrePersist
    @PreUpdate
    void onWrite() {
        this.updatedAt = Instant.now();
    }
}
