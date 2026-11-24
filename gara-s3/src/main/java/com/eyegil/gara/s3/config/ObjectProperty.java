package com.eyegil.gara.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Object limits.
 *
 * @param maxSize         largest object, plain or assembled from multipart parts; each object is one file on GitHub,
 *                        which rejects files above 100MB
 * @param multipartExpiry multipart uploads left unfinished this long are aborted and their parts dropped
 */
@ConfigurationProperties("gara.object")
public record ObjectProperty(
        @DefaultValue("100MB") DataSize maxSize,
        @DefaultValue("7d") Duration multipartExpiry
) {
}
