package com.eyegil.gara.catalog.lock;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.coordination.model.Lease;

/**
 * Exclusive right to commit to one volume's repository. Close it to let the next writer in.
 */
public final class VolumeLease implements AutoCloseable {

    private final Volume volume;
    private final Lease lease;

    VolumeLease(Volume volume, Lease lease) {
        this.volume = volume;
        this.lease = lease;
    }

    public Volume volume() {
        return volume;
    }

    @Override
    public void close() {
        lease.close();
    }
}
