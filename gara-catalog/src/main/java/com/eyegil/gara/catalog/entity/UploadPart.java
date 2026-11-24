package com.eyegil.gara.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A part of a {@link MultipartUpload}, held in Postgres until the upload completes or is aborted.
 */
@Entity
@Table(name = "gara_upload_part")
@Getter
@Setter
@NoArgsConstructor
public class UploadPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", nullable = false, length = 36)
    private String uploadId;

    @Column(name = "part_number", nullable = false)
    private int partNumber;

    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "size", nullable = false)
    private long size;

    /**
     * Unquoted MD5 hex of the part.
     */
    @Column(name = "etag", nullable = false, length = 32)
    private String etag;

    public UploadPart(String uploadId, int partNumber) {
        this.uploadId = uploadId;
        this.partNumber = partNumber;
    }
}
