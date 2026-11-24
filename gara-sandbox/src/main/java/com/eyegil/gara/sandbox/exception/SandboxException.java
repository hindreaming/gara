package com.eyegil.gara.sandbox.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * An upload the sandbox refuses; the message is shown to the visitor as is.
 */
@Getter
public class SandboxException extends RuntimeException {

    private final HttpStatus status;

    public SandboxException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
