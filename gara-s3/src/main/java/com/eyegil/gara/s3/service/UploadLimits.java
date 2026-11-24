package com.eyegil.gara.s3.service;

import com.eyegil.gara.s3.config.ObjectProperty;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Size rules for request bodies: a plain upload or a multipart part can be at most the object size limit.
 */
@Component
@RequiredArgsConstructor
public class UploadLimits {

    private static final int MAX_PART_NUMBER = 10_000;

    private final ObjectProperty objectProperty;

    public void requireStorableSize(byte[] content) {
        if (content.length > objectProperty.maxSize().toBytes()) {
            throw new S3Exception(S3ErrorCode.ENTITY_TOO_LARGE);
        }
    }

    public void requireValidPartNumber(int partNumber) {
        if (partNumber < 1 || partNumber > MAX_PART_NUMBER) {
            throw new S3Exception(S3ErrorCode.INVALID_ARGUMENT, "Part number must be between 1 and 10000.");
        }
    }
}
