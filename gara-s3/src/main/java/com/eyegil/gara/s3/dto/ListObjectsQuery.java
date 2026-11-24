package com.eyegil.gara.s3.dto;

/**
 * Query parameters of ListObjects; {@code v2} is true for {@code list-type=2}.
 */
public record ListObjectsQuery(
        boolean v2,
        String prefix,
        String delimiter,
        int maxKeys,
        String marker,
        String continuationToken,
        String startAfter,
        String encodingType
) {
}
