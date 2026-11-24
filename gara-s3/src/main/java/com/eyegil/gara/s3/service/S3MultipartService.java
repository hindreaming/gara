package com.eyegil.gara.s3.service;

import com.eyegil.gara.catalog.entity.MultipartUpload;
import com.eyegil.gara.catalog.entity.UploadPart;
import com.eyegil.gara.catalog.service.MultipartUploadService;
import com.eyegil.gara.s3.config.ObjectProperty;
import com.eyegil.gara.s3.dto.CompleteMultipartUpload.CompletedPart;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.s3.util.ETags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * S3 multipart uploads. Parts wait in Postgres; completing the upload joins them and stores the result as one
 * file, like a plain upload, so the joined object is subject to the same size limit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3MultipartService {

    private final ObjectProperty objectProperty;
    private final UploadLimits uploadLimits;
    private final S3BucketService s3BucketService;
    private final MultipartUploadService multipartUploadService;
    private final S3ObjectService s3ObjectService;

    public String initiate(String bucketId, String key, String contentType) {
        s3BucketService.requireExists(bucketId);
        String type = StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return multipartUploadService.create(bucketId, key, type).getId();
    }

    /**
     * @return the part's unquoted ETag
     */
    public String uploadPart(String bucketId, String key, String uploadId, int partNumber, byte[] content) {
        uploadLimits.requireValidPartNumber(partNumber);
        uploadLimits.requireStorableSize(content);
        requireUpload(bucketId, key, uploadId);

        String etag = ETags.of(content);
        multipartUploadService.savePart(uploadId, partNumber, content, etag);
        return etag;
    }

    /**
     * @return the object's unquoted ETag
     */
    public String complete(String bucketId, String key, String uploadId, List<CompletedPart> requestedParts) {
        MultipartUpload upload = requireUpload(bucketId, key, uploadId);
        List<UploadPart> chosen = chooseParts(upload, requestedParts);
        long size = chosen.stream().mapToLong(UploadPart::getSize).sum();
        if (size > objectProperty.maxSize().toBytes()) {
            throw new S3Exception(S3ErrorCode.ENTITY_TOO_LARGE);
        }

        ByteArrayOutputStream content = new ByteArrayOutputStream((int) size);
        chosen.forEach(part -> content.writeBytes(part.getContent()));
        String etag = ETags.ofParts(chosen.stream().map(UploadPart::getEtag).toList());
        s3ObjectService.store(bucketId, key, content.toByteArray(), etag, upload.getContentType());
        multipartUploadService.remove(upload);
        return etag;
    }

    public void abort(String bucketId, String key, String uploadId) {
        multipartUploadService.remove(requireUpload(bucketId, key, uploadId));
    }

    /**
     * Drops uploads older than the configured expiry, so abandoned parts do not pile up in Postgres.
     */
    public void abortExpired() {
        Instant cutoff = Instant.now().minus(objectProperty.multipartExpiry());
        for (MultipartUpload upload : multipartUploadService.findStartedBefore(cutoff)) {
            multipartUploadService.remove(upload);
            log.info("Aborted expired multipart upload {} of {}/{}", upload.getId(), upload.getBucketId(), upload.getObjectKey());
        }
    }

    private List<UploadPart> chooseParts(MultipartUpload upload, List<CompletedPart> requestedParts) {
        if (requestedParts.isEmpty()) {
            throw new S3Exception(S3ErrorCode.MALFORMED_XML, "The upload must list at least one part.");
        }
        Map<Integer, UploadPart> uploaded = multipartUploadService.parts(upload.getId()).stream()
                .collect(Collectors.toMap(UploadPart::getPartNumber, Function.identity()));

        List<UploadPart> chosen = new ArrayList<>();
        int previous = 0;
        for (CompletedPart requested : requestedParts) {
            if (requested.getPartNumber() <= previous) {
                throw new S3Exception(S3ErrorCode.INVALID_PART_ORDER);
            }
            UploadPart part = uploaded.get(requested.getPartNumber());
            if (part == null || requested.getEtag() == null || !part.getEtag().equals(ETags.unquote(requested.getEtag()))) {
                throw new S3Exception(S3ErrorCode.INVALID_PART);
            }
            chosen.add(part);
            previous = requested.getPartNumber();
        }
        return chosen;
    }

    private MultipartUpload requireUpload(String bucketId, String key, String uploadId) {
        s3BucketService.requireExists(bucketId);
        return multipartUploadService.find(uploadId)
                .filter(upload -> upload.getBucketId().equals(bucketId) && upload.getObjectKey().equals(key))
                .orElseThrow(() -> new S3Exception(S3ErrorCode.NO_SUCH_UPLOAD));
    }
}
