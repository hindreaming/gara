package com.eyegil.gara.github.service;

import com.eyegil.gara.coordination.service.SharedRateLimiter;
import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.gara.github.dto.WriteBudget;
import com.eyegil.gara.github.exception.GithubWriteThrottledException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Paces every commit Gara sends to GitHub, across all repositories and all instances, so the account stays
 * under GitHub's secondary rate limits instead of getting temporarily blocked.
 */
@Component
@RequiredArgsConstructor
public class GithubWriteThrottle {

    private static final String BUDGET = "github-writes";

    private final SharedRateLimiter sharedRateLimiter;
    private final GithubProperty githubProperty;

    /**
     * Blocks until a commit may be sent, or throws when that would take longer than the throttle wait.
     */
    public void acquire() {
        Duration wait = sharedRateLimiter
                .reserve(BUDGET, githubProperty.writesPerHour(), githubProperty.writeBurst(), githubProperty.throttleWait())
                .orElseThrow(GithubWriteThrottledException::new);
        if (wait.isZero()) {
            return;
        }
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GithubWriteThrottledException();
        }
    }

    public WriteBudget budget() {
        double available = sharedRateLimiter.available(BUDGET, githubProperty.writesPerHour(), githubProperty.writeBurst());
        return new WriteBudget(available, githubProperty.writeBurst(), githubProperty.writesPerHour());
    }
}
