package com.eyegil.gara.security.exception;

import lombok.Getter;

@Getter
public class SignatureVerificationException extends RuntimeException {

    private final SignatureFailure failure;

    public SignatureVerificationException(SignatureFailure failure, String message) {
        super(message);
        this.failure = failure;
    }
}
