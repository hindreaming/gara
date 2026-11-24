package com.eyegil.gara.admin.dto;

/**
 * @param repository the bucket's first repository; later ones are {@code repository-2}, ...
 * @param directory  folder inside each repository that holds the objects ("" for the root)
 * @param publicRepos the bucket's repositories are public on GitHub
 * @param totalSize  bytes of the current object versions (what clients can download), not history
 */
public record BucketView(String id, String repository, String directory, boolean publicRepos,
                         long objects, long totalSize, long activeVolumes) {
}
