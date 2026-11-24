package com.eyegil.gara.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Slows down password guessing: a client address that fails {@code maxFailures} times within {@code duration} is
 * refused for {@code duration}. Kept per instance; behind a proxy, enable {@code server.forward-headers-strategy}
 * so the real client address is seen.
 */
@ConfigurationProperties("gara.auth.lockout")
public record LockoutProperty(
        @DefaultValue("10") int maxFailures,
        @DefaultValue("15m") Duration duration
) {
}
