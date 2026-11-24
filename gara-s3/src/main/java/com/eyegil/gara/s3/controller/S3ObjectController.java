package com.eyegil.gara.s3.controller;

import com.eyegil.gara.s3.dto.CopyObjectResult;
import com.eyegil.gara.s3.dto.S3ObjectMetadata;
import com.eyegil.gara.s3.dto.S3Payload;
import com.eyegil.gara.s3.dto.S3StoredObject;
import com.eyegil.gara.s3.service.S3ObjectService;
import com.eyegil.gara.s3.util.CopySource;
import com.eyegil.gara.s3.util.ObjectKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.temporal.ChronoUnit;

/**
 * S3 object endpoints (path-style): PutObject, CopyObject, GetObject (with Range), HeadObject, DeleteObject.
 */
@RestController
@RequiredArgsConstructor
public class S3ObjectController {

    static final String OBJECT_PATH = "/{bucket}/{*key}";
    private static final String COPY_SOURCE = "x-amz-copy-source";
    private static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";
    private static final String REPLACE = "REPLACE";

    private final S3ObjectService s3ObjectService;

    @PutMapping(value = OBJECT_PATH, consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> putObject(@PathVariable String bucket,
                                          @PathVariable String key,
                                          S3Payload payload,
                                          @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        S3ObjectMetadata stored = s3ObjectService.put(bucket, ObjectKeys.fromPath(key), payload.bytes(), contentType);
        return ResponseEntity.ok().eTag(stored.etag()).build();
    }

    @PutMapping(value = OBJECT_PATH, headers = COPY_SOURCE, consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public CopyObjectResult copyObject(@PathVariable String bucket,
                                       @PathVariable String key,
                                       @RequestHeader(COPY_SOURCE) String copySource,
                                       @RequestHeader(value = METADATA_DIRECTIVE, required = false) String metadataDirective,
                                       @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        CopySource source = CopySource.parse(copySource);
        String replacementType = REPLACE.equalsIgnoreCase(metadataDirective) ? contentType : null;
        S3ObjectMetadata copied = s3ObjectService.copy(source.bucket(), source.key(), bucket, ObjectKeys.fromPath(key), replacementType);
        return new CopyObjectResult(copied.lastModified().truncatedTo(ChronoUnit.MILLIS).toString(), "\"" + copied.etag() + "\"");
    }

    /**
     * Returning a {@link Resource} lets Spring MVC answer {@code Range} requests with 206 and the requested bytes.
     */
    @GetMapping(OBJECT_PATH)
    public ResponseEntity<Resource> getObject(@PathVariable String bucket, @PathVariable String key) {
        S3StoredObject object = s3ObjectService.get(bucket, ObjectKeys.fromPath(key));
        S3ObjectMetadata metadata = object.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .lastModified(metadata.lastModified())
                .eTag(metadata.etag())
                .body(new ByteArrayResource(object.content()));
    }

    @RequestMapping(value = OBJECT_PATH, method = RequestMethod.HEAD)
    public ResponseEntity<Void> headObject(@PathVariable String bucket, @PathVariable String key) {
        S3ObjectMetadata metadata = s3ObjectService.head(bucket, ObjectKeys.fromPath(key));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .contentLength(metadata.size())
                .lastModified(metadata.lastModified())
                .eTag(metadata.etag())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .build();
    }

    @DeleteMapping(OBJECT_PATH)
    public ResponseEntity<Void> deleteObject(@PathVariable String bucket, @PathVariable String key) {
        s3ObjectService.delete(bucket, ObjectKeys.fromPath(key));
        return ResponseEntity.noContent().build();
    }
}
