package com.eyegil.gara.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param maxConcurrentUploads uploads (PUT/POST bodies) handled at once per instance; each body is held in memory,
 *                             so this times the object size limit bounds the memory uploads can take
 * @param uploadWait           how long an upload waits for a slot before failing with SlowDown
 */
@ConfigurationProperties("gara.s3")
public record S3Property(
        @DefaultValue("8") int maxConcurrentUploads,
        @DefaultValue("30s") Duration uploadWait
) {

    public S3Property {
        if (maxConcurrentUploads < 1) {
            throw new IllegalArgumentException("gara.s3.max-concurrent-uploads must be positive");
        }
    }
}
