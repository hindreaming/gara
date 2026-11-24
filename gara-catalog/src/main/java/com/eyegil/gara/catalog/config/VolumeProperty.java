package com.eyegil.gara.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Capacity and pool rules for the GitHub repositories ("volumes") of a bucket.
 * {@code pool-check-interval} is also read directly by the pool scheduler, so keep it in ISO format (PT5M).
 *
 * @param maxSize           repository size GitHub is comfortable with
 * @param fillThreshold     fraction of {@code maxSize} at which a volume stops taking writes
 * @param maxFiles          file count at which a volume stops taking writes (recursive git trees truncate at 100k entries)
 * @param minActive         active volumes with room to spare that the pool scheduler keeps per bucket
 * @param maxActive         most active volumes a bucket may have
 * @param nearlyFull        fraction of a volume's limits (size and files) from which it no longer counts as having
 *                          room to spare, so its replacement is created before it fills up
 * @param poolCheckInterval how often the pool scheduler checks each bucket (Postgres only; GitHub is called only to
 *                          create a repository)
 * @param leaseTimeout      how long an upload waits for a free volume before failing with SlowDown
 */
@ConfigurationProperties("gara.volume")
public record VolumeProperty(
        @DefaultValue("1GB") DataSize maxSize,
        @DefaultValue("0.9") double fillThreshold,
        @DefaultValue("50000") long maxFiles,
        @DefaultValue("2") int minActive,
        @DefaultValue("3") int maxActive,
        @DefaultValue("0.8") double nearlyFull,
        @DefaultValue("PT5M") Duration poolCheckInterval,
        @DefaultValue("60s") Duration leaseTimeout
) {

    public VolumeProperty {
        if (minActive < 1 || maxActive < minActive) {
            throw new IllegalArgumentException("gara.volume requires 1 <= min-active <= max-active");
        }
        if (fillThreshold <= 0 || fillThreshold > 1) {
            throw new IllegalArgumentException("gara.volume.fill-threshold must be in (0, 1]");
        }
        if (nearlyFull <= 0 || nearlyFull > 1) {
            throw new IllegalArgumentException("gara.volume.nearly-full must be in (0, 1]");
        }
    }

    public long fillLimitBytes() {
        return (long) (maxSize.toBytes() * fillThreshold);
    }
}
