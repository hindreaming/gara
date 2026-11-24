package com.eyegil.gara.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Where Gara writes on GitHub and how fast. Every upload and delete is a commit, and GitHub's secondary rate limits
 * cap content-creating requests per account (documented as roughly 80 per minute and 500 per hour), no matter how
 * many repositories they are spread over. The API token itself is {@code github.token}, read by github-client.
 *
 * @param owner         user or organization that owns the repositories
 * @param branch        branch every file is committed to
 * @param writesPerHour sustained commit rate across all repositories and instances
 * @param writeBurst    commits that may be sent back to back before pacing kicks in
 * @param throttleWait  longest an upload waits for a commit slot before failing with SlowDown
 * @param writeRetries  extra attempts for a commit that hit a conflict, a rate limit or a GitHub outage
 * @param retryBackoff  wait before the first retry, multiplied by the attempt number afterwards
 * @param verifyOnStartup check the token and every bucket's first repository at startup and refuse to start if
 *                      they are not usable
 */
@ConfigurationProperties("gara.github")
public record GithubProperty(
        String owner,
        @DefaultValue("main") String branch,
        @DefaultValue("450") int writesPerHour,
        @DefaultValue("20") int writeBurst,
        @DefaultValue("60s") Duration throttleWait,
        @DefaultValue("3") int writeRetries,
        @DefaultValue("2s") Duration retryBackoff,
        @DefaultValue("true") boolean verifyOnStartup
) {

    public GithubProperty {
        if (!StringUtils.hasText(owner)) {
            throw new IllegalArgumentException("gara.github.owner must be set (GITHUB_OWNER)");
        }
        if (writesPerHour < 1 || writeBurst < 1) {
            throw new IllegalArgumentException("gara.github writes-per-hour and write-burst must be positive");
        }
    }
}
