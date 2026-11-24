package com.eyegil.gara.s3.service;

import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.s3.dto.ListAllMyBucketsResult;
import com.eyegil.gara.s3.dto.ListAllMyBucketsResult.BucketEntry;
import com.eyegil.gara.s3.dto.ListAllMyBucketsResult.Owner;
import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.security.config.AuthProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class S3BucketService {

    private final BucketService bucketService;
    private final AuthProperty authProperty;

    public void requireExists(String bucketId) {
        if (!bucketService.exists(bucketId)) {
            throw new S3Exception(S3ErrorCode.NO_SUCH_BUCKET);
        }
    }

    public ListAllMyBucketsResult listBuckets() {
        return new ListAllMyBucketsResult(
                new Owner(authProperty.username(), authProperty.username()),
                bucketService.findAll().stream()
                        .map(bucket -> new BucketEntry(bucket.getName(),
                                bucket.getCreatedAt().truncatedTo(ChronoUnit.MILLIS).toString()))
                        .toList());
    }
}
