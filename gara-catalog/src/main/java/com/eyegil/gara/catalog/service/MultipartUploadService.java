package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.entity.MultipartUpload;
import com.eyegil.gara.catalog.entity.UploadPart;
import com.eyegil.gara.catalog.repository.MultipartUploadRepository;
import com.eyegil.gara.catalog.repository.UploadPartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Multipart uploads in progress. Parts are kept in Postgres, so they reach the same place whichever instance
 * receives them; nothing goes to GitHub until the upload completes.
 */
@Service
@RequiredArgsConstructor
public class MultipartUploadService {

    private final MultipartUploadRepository multipartUploadRepository;
    private final UploadPartRepository uploadPartRepository;

    @Transactional
    public MultipartUpload create(String bucketId, String objectKey, String contentType) {
        return multipartUploadRepository.save(new MultipartUpload(bucketId, objectKey, contentType));
    }

    public Optional<MultipartUpload> find(String uploadId) {
        return multipartUploadRepository.findById(uploadId);
    }

    /**
     * Stores a part. Uploading the same part number again replaces it.
     */
    @Transactional
    public void savePart(String uploadId, int partNumber, byte[] content, String etag) {
        UploadPart part = uploadPartRepository.findByUploadIdAndPartNumber(uploadId, partNumber)
                .orElseGet(() -> new UploadPart(uploadId, partNumber));
        part.setContent(content);
        part.setSize(content.length);
        part.setEtag(etag);
        uploadPartRepository.save(part);
    }

    /**
     * Parts with their content, in part-number order.
     */
    public List<UploadPart> parts(String uploadId) {
        return uploadPartRepository.findAllByUploadIdOrderByPartNumberAsc(uploadId);
    }

    /**
     * Ends the upload and drops its parts.
     */
    @Transactional
    public void remove(MultipartUpload upload) {
        multipartUploadRepository.delete(upload);
    }

    public List<MultipartUpload> findStartedBefore(Instant cutoff) {
        return multipartUploadRepository.findAllByCreatedAtBefore(cutoff);
    }

    public long count() {
        return multipartUploadRepository.count();
    }
}
