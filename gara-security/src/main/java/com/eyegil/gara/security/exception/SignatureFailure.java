package com.eyegil.gara.security.exception;

/**
 * Why a request's AWS signature was rejected.
 */
public enum SignatureFailure {
    MISSING,
    MALFORMED,
    UNKNOWN_ACCESS_KEY,
    MISMATCH,
    EXPIRED,
    CLOCK_SKEW
}
