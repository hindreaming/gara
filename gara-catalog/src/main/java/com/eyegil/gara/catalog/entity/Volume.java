package com.eyegil.gara.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One GitHub repository that holds part of a bucket. New keys spread over the bucket's ACTIVE volumes; a volume
 * that fills up becomes FULL and takes no new keys (keys it already holds are still replaced in place), and the
 * pool is topped up with a new repository.
 * The schema is owned by the Flyway migrations in {@code db/migration}.
 */
@Entity
@Table(name = "gara_volume")
@Getter
@Setter
@NoArgsConstructor
public class Volume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_id", nullable = false)
    private String bucketId;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "directory", nullable = false)
    private String directory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VolumeStatus status;

    /**
     * Bytes ever committed to the repository. Never decreases, because deleted and replaced files stay in git history.
     */
    @Column(name = "written_bytes", nullable = false)
    private long writtenBytes;

    /**
     * Files currently at the branch head.
     */
    @Column(name = "head_files", nullable = false)
    private long headFiles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Volume(String bucketId, String repoName, String directory) {
        this.bucketId = bucketId;
        this.repoName = repoName;
        this.directory = directory;
        this.status = VolumeStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
