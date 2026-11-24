package com.eyegil.gara.coordination.model;

import java.util.function.Consumer;

/**
 * Exclusive hold on a named resource, shared across Gara instances. Close it to release.
 */
public final class Lease implements AutoCloseable {

    private final String name;
    private final String ownerId;
    private final Consumer<Lease> releaser;

    public Lease(String name, String ownerId, Consumer<Lease> releaser) {
        this.name = name;
        this.ownerId = ownerId;
        this.releaser = releaser;
    }

    public String name() {
        return name;
    }

    public String ownerId() {
        return ownerId;
    }

    @Override
    public void close() {
        releaser.accept(this);
    }
}
