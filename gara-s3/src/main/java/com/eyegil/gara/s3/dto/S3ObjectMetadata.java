package com.eyegil.gara.s3.dto;

import java.time.Instant;

/**
 * Object metadata returned in HEAD/GET headers. {@code etag} is unquoted.
 */
public record S3ObjectMetadata(
        String key,
        long size,
        String etag,
        String contentType,
        Instant lastModified
) {
}
