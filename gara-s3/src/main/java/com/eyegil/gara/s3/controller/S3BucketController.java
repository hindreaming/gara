package com.eyegil.gara.s3.controller;

import com.eyegil.gara.s3.dto.DeleteObjectsRequest;
import com.eyegil.gara.s3.dto.DeleteResult;
import com.eyegil.gara.s3.dto.ListAllMyBucketsResult;
import com.eyegil.gara.s3.dto.ListBucketResult;
import com.eyegil.gara.s3.dto.ListObjectsQuery;
import com.eyegil.gara.s3.dto.S3Payload;
import com.eyegil.gara.s3.service.S3BatchDeleteService;
import com.eyegil.gara.s3.service.S3BucketService;
import com.eyegil.gara.s3.service.S3ListingService;
import com.eyegil.gara.s3.util.S3Xml;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 bucket endpoints (path-style): ListBuckets, HeadBucket, ListObjects / ListObjectsV2 and DeleteObjects.
 */
@RestController
@RequiredArgsConstructor
public class S3BucketController {

    private static final int LIST_TYPE_V2 = 2;
    private static final String BUCKET_PATH = "/{bucket}";
    private static final String BUCKET_PATH_SLASH = "/{bucket}/";

    private final S3BucketService s3BucketService;
    private final S3ListingService s3ListingService;
    private final S3BatchDeleteService s3BatchDeleteService;
    private final S3Xml s3Xml;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_XML_VALUE)
    public ListAllMyBucketsResult listBuckets() {
        return s3BucketService.listBuckets();
    }

    @RequestMapping(value = {BUCKET_PATH, BUCKET_PATH_SLASH}, method = RequestMethod.HEAD)
    public ResponseEntity<Void> headBucket(@PathVariable String bucket) {
        s3BucketService.requireExists(bucket);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = {BUCKET_PATH, BUCKET_PATH_SLASH}, produces = MediaType.APPLICATION_XML_VALUE)
    public ListBucketResult listObjects(@PathVariable String bucket,
                                        @RequestParam(name = "list-type", required = false) Integer listType,
                                        @RequestParam(name = "prefix", required = false) String prefix,
                                        @RequestParam(name = "delimiter", required = false) String delimiter,
                                        @RequestParam(name = "max-keys", defaultValue = "1000") int maxKeys,
                                        @RequestParam(name = "marker", required = false) String marker,
                                        @RequestParam(name = "continuation-token", required = false) String continuationToken,
                                        @RequestParam(name = "start-after", required = false) String startAfter,
                                        @RequestParam(name = "encoding-type", required = false) String encodingType) {
        ListObjectsQuery query = new ListObjectsQuery(
                listType != null && listType == LIST_TYPE_V2,
                prefix, delimiter, maxKeys, marker, continuationToken, startAfter, encodingType);
        return s3ListingService.list(bucket, query);
    }

    @PostMapping(value = {BUCKET_PATH, BUCKET_PATH_SLASH}, params = "delete", consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public DeleteResult deleteObjects(@PathVariable String bucket, S3Payload payload) {
        return s3BatchDeleteService.deleteAll(bucket, s3Xml.read(payload.bytes(), DeleteObjectsRequest.class));
    }
}
