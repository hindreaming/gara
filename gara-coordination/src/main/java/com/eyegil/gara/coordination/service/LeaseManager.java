package com.eyegil.gara.coordination.service;

import com.eyegil.gara.coordination.dto.ActiveLease;
import com.eyegil.gara.coordination.model.Lease;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Named, expiring, exclusive leases shared by every Gara instance. Each call hands out a distinct owner,
 * so two threads of one instance exclude each other just like two instances do.
 */
public interface LeaseManager {

    /**
     * Takes the lease if nobody holds it (or the holder's lease expired).
     */
    Optional<Lease> tryAcquire(String name);

    /**
     * Retries {@link #tryAcquire} until it succeeds or {@code timeout} passes.
     */
    Optional<Lease> acquire(String name, Duration timeout);

    /**
     * Unexpired leases of all instances, by name.
     */
    List<ActiveLease> active();
}
