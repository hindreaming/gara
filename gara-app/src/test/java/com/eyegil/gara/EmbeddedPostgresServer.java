package com.eyegil.gara;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * One real Postgres server per test JVM, started on first use and stopped when the JVM exits.
 * Runs from bundled binaries, so tests need neither Docker nor a local Postgres installation.
 */
final class EmbeddedPostgresServer {

    private static EmbeddedPostgres server;

    private EmbeddedPostgresServer() {
    }

    static synchronized String jdbcUrl() {
        if (server == null) {
            server = start();
        }
        return server.getJdbcUrl("postgres", "postgres");
    }

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres started = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    started.close();
                } catch (IOException ignored) {
                    // the JVM is exiting; the temporary data directory is removed by the OS eventually
                }
            }));
            return started;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded Postgres", e);
        }
    }
}
