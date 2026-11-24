package com.eyegil.gara.catalog.exception;

/**
 * No volume of the bucket became free for writing within the lease timeout.
 */
public class VolumePoolBusyException extends RuntimeException {

    public VolumePoolBusyException(String bucketId) {
        super("Every active volume of bucket " + bucketId + " is busy");
    }
}
