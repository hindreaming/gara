package com.eyegil.gara.catalog.exception;

import lombok.Getter;

/**
 * A bucket request that breaks a rule: an invalid name, a name in use, a missing repository, a non-empty bucket.
 */
@Getter
public class BucketRuleException extends RuntimeException {

    private final Reason reason;

    public BucketRuleException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public enum Reason {
        INVALID,
        CONFLICT,
        NOT_FOUND
    }
}
