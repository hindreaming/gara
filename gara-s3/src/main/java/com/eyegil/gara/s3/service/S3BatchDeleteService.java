package com.eyegil.gara.s3.service;

import com.eyegil.gara.s3.dto.DeleteObjectsRequest;
import com.eyegil.gara.s3.dto.DeleteObjectsRequest.ObjectIdentifier;
import com.eyegil.gara.s3.dto.DeleteResult;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.s3.util.ObjectKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * DeleteObjects: deletes up to 1000 keys, each on its own, and reports per key what happened.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3BatchDeleteService {

    private static final int MAX_KEYS = 1000;

    private final S3BucketService s3BucketService;
    private final S3ObjectService s3ObjectService;

    public DeleteResult deleteAll(String bucketId, DeleteObjectsRequest request) {
        s3BucketService.requireExists(bucketId);
        if (request.getObjects().isEmpty() || request.getObjects().size() > MAX_KEYS) {
            throw new S3Exception(S3ErrorCode.MALFORMED_XML, "A delete request must name between 1 and 1000 keys.");
        }

        List<DeleteResult.Deleted> deleted = new ArrayList<>();
        List<DeleteResult.Failed> failed = new ArrayList<>();
        for (ObjectIdentifier object : request.getObjects()) {
            String key = object.getKey();
            try {
                s3ObjectService.delete(bucketId, ObjectKeys.fromPath(key));
                if (!request.isQuiet()) {
                    deleted.add(new DeleteResult.Deleted(key));
                }
            } catch (S3Exception e) {
                failed.add(new DeleteResult.Failed(key, e.getErrorCode().getCode(), e.getMessage()));
            } catch (RuntimeException e) {
                log.error("Deleting {} from {} failed", key, bucketId, e);
                failed.add(new DeleteResult.Failed(key, S3ErrorCode.INTERNAL_ERROR.getCode(),
                        S3ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
            }
        }
        return new DeleteResult(deleted, failed);
    }
}
