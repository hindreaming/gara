package com.eyegil.gara.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * An S3 multipart upload in progress. Its parts wait in Postgres; completing it joins them into one file.
 */
@Entity
@Table(name = "gara_multipart_upload")
@Getter
@Setter
@NoArgsConstructor
public class MultipartUpload {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "bucket_id", nullable = false)
    private String bucketId;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MultipartUpload(String bucketId, String objectKey, String contentType) {
        this.id = UUID.randomUUID().toString();
        this.bucketId = bucketId;
        this.objectKey = objectKey;
        this.contentType = contentType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
