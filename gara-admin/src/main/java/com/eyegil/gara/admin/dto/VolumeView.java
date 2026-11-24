package com.eyegil.gara.admin.dto;

/**
 * @param writtenBytes bytes ever committed, history included; this is what fills a repository
 * @param fillPercent  {@code writtenBytes} as a share of the volume's fill limit
 * @param busy         a write lease is currently held on the repository by some instance
 */
public record VolumeView(
        Long id,
        String bucketId,
        String repoName,
        String directory,
        String status,
        long writtenBytes,
        long headFiles,
        double fillPercent,
        boolean busy
) {
}
