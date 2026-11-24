package com.eyegil.gara.coordination.exception;

/**
 * A lease stayed taken by another holder for longer than the caller was willing to wait.
 */
public class LeaseTimeoutException extends RuntimeException {

    public LeaseTimeoutException(String leaseName) {
        super("Timed out waiting for lease " + leaseName);
    }
}
