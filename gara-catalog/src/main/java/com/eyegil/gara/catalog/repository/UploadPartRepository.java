package com.eyegil.gara.catalog.repository;

import com.eyegil.gara.catalog.entity.UploadPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadPartRepository extends JpaRepository<UploadPart, Long> {

    Optional<UploadPart> findByUploadIdAndPartNumber(String uploadId, int partNumber);

    List<UploadPart> findAllByUploadIdOrderByPartNumberAsc(String uploadId);
}
