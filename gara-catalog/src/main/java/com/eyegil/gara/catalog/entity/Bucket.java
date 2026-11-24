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

/**
 * An S3 bucket. It starts on {@code repository} (its first volume); later volumes are {@code repository-2}, ...
 * Objects live under {@code directory} inside every repository of the bucket ("" for the repository root).
 */
@Entity
@Table(name = "gara_bucket")
@Getter
@Setter
@NoArgsConstructor
public class Bucket {

    @Id
    @Column(name = "name", length = 63)
    private String name;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "directory", nullable = false)
    private String directory;

    /**
     * Whether repositories Gara creates for this bucket are public (anyone can read the files on GitHub).
     */
    @Column(name = "public_repos", nullable = false)
    private boolean publicRepos;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Bucket(String name, String repository, String directory, boolean publicRepos) {
        this.name = name;
        this.repository = repository;
        this.directory = directory;
        this.publicRepos = publicRepos;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
