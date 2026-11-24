package com.eyegil.gara.admin.dto;

import java.util.List;

/**
 * One folder level of a bucket, MinIO-console style: sub-folders and the objects directly inside {@code prefix}.
 *
 * @param truncated more entries exist than were returned
 */
public record BrowseResponse(
        String bucket,
        String prefix,
        List<String> folders,
        List<ObjectEntry> objects,
        boolean truncated
) {
}
