package com.eyegil.gara.admin.exception;

import com.eyegil.gara.admin.controller.AdminObjectController;
import com.eyegil.gara.admin.dto.AdminError;
import com.eyegil.gara.catalog.exception.BucketRuleException;
import com.eyegil.gara.catalog.exception.VolumePoolBusyException;
import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.github.exception.GithubWriteThrottledException;
import com.eyegil.gara.s3.exception.S3Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * JSON errors for the admin API, which reuses the S3 and catalog services and their exceptions.
 */
@RestControllerAdvice(basePackageClasses = AdminObjectController.class)
public class AdminExceptionHandler {

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<AdminError> handleS3Exception(S3Exception e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new AdminError(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(BucketRuleException.class)
    public ResponseEntity<AdminError> handleBucketRule(BucketRuleException e) {
        HttpStatus status = switch (e.getReason()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(new AdminError(e.getReason().name(), e.getMessage()));
    }

    @ExceptionHandler({VolumePoolBusyException.class, LeaseTimeoutException.class, GithubWriteThrottledException.class})
    public ResponseEntity<AdminError> handleBackPressure(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new AdminError("SlowDown", e.getMessage()));
    }
}
