package com.eyegil.gara.sandbox.exception;

import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.sandbox.controller.SandboxController;
import com.eyegil.gara.sandbox.dto.SandboxError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = SandboxController.class)
public class SandboxExceptionHandler {

    @ExceptionHandler(SandboxException.class)
    public ResponseEntity<SandboxError> refused(SandboxException e) {
        return ResponseEntity.status(e.getStatus()).body(new SandboxError(e.getMessage()));
    }

    @ExceptionHandler(LeaseTimeoutException.class)
    public ResponseEntity<SandboxError> busy(LeaseTimeoutException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new SandboxError("The sandbox is busy with other uploads. Try again in a minute."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SandboxError> failed(Exception e) {
        log.warn("Sandbox upload failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new SandboxError("The upload could not be stored. Try again later."));
    }
}
