package com.eyegil.gara.coordination.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Token buckets stored in {@code gara_rate_budget}, so every instance draws from the same budget.
 * The refill uses the database clock, which keeps instances with skewed clocks consistent.
 */
@Service
@RequiredArgsConstructor
public class SharedRateLimiter {

    private static final double SECONDS_PER_HOUR = 3600;

    private static final String CREATE = """
            INSERT INTO gara_rate_budget (name, tokens, refilled_at) VALUES (?, ?, now())
            ON CONFLICT (name) DO NOTHING
            """;
    private static final String LOCK = """
            SELECT tokens, EXTRACT(EPOCH FROM (now() - refilled_at)) AS elapsed
            FROM gara_rate_budget WHERE name = ? FOR UPDATE
            """;
    private static final String PEEK = """
            SELECT tokens, EXTRACT(EPOCH FROM (now() - refilled_at)) AS elapsed FROM gara_rate_budget WHERE name = ?
            """;
    private static final String SPEND = "UPDATE gara_rate_budget SET tokens = ?, refilled_at = now() WHERE name = ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * Reserves one token from budget {@code name}, refilled at {@code perHour} up to {@code burst} tokens.
     *
     * @return how long the caller must wait before acting, or empty (nothing reserved) if that exceeds {@code maxWait}
     */
    public Optional<Duration> reserve(String name, int perHour, int burst, Duration maxWait) {
        double tokensPerSecond = perHour / SECONDS_PER_HOUR;
        return transactionTemplate.execute(status -> {
            jdbcTemplate.update(CREATE, name, (double) burst);
            Map<String, Object> row = jdbcTemplate.queryForMap(LOCK, name);
            double elapsedSeconds = ((Number) row.get("elapsed")).doubleValue();
            double tokens = Math.min(burst, ((Number) row.get("tokens")).doubleValue() + elapsedSeconds * tokensPerSecond);

            Duration wait = tokens >= 1
                    ? Duration.ZERO
                    : Duration.ofNanos((long) Math.ceil((1 - tokens) / tokensPerSecond * 1_000_000_000L));
            if (wait.compareTo(maxWait) > 0) {
                return Optional.empty();
            }
            jdbcTemplate.update(SPEND, tokens - 1, name);
            return Optional.of(wait);
        });
    }

    /**
     * Tokens currently in budget {@code name}, without spending any; may be negative while callers wait.
     */
    public double available(String name, int perHour, int burst) {
        return jdbcTemplate.query(PEEK, rs -> rs.next()
                ? Math.min(burst, rs.getDouble("tokens") + rs.getDouble("elapsed") * perHour / SECONDS_PER_HOUR)
                : burst, name);
    }
}
