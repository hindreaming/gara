package com.eyegil.gara.catalog.repository;

import com.eyegil.gara.catalog.entity.MultipartUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MultipartUploadRepository extends JpaRepository<MultipartUpload, String> {

    List<MultipartUpload> findAllByCreatedAtBefore(Instant cutoff);
}
