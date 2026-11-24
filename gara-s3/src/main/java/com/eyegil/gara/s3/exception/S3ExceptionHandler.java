package com.eyegil.gara.s3.exception;

import com.eyegil.gara.catalog.exception.VolumePoolBusyException;
import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.github.exception.GithubWriteThrottledException;
import com.eyegil.gara.s3.controller.S3ObjectController;
import com.eyegil.gara.s3.dto.S3Error;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders failures of the S3 controllers as S3 XML error documents. Scoped to the S3 controller package
 * so other endpoints keep Spring's default error handling.
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = S3ObjectController.class)
public class S3ExceptionHandler {

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<S3Error> handleS3Exception(S3Exception e, HttpServletRequest request) {
        S3ErrorCode errorCode = e.getErrorCode();
        return error(errorCode.getCode(), errorCode.getStatus(), e.getMessage(), request);
    }

    /**
     * Back-pressure: every volume or the object key is busy, or the GitHub commit budget is used up. S3 clients retry SlowDown.
     */
    @ExceptionHandler({VolumePoolBusyException.class, LeaseTimeoutException.class, GithubWriteThrottledException.class})
    public ResponseEntity<S3Error> handleBackPressure(RuntimeException e, HttpServletRequest request) {
        log.warn("S3 request {} {} rejected: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        S3ErrorCode errorCode = S3ErrorCode.SLOW_DOWN;
        return error(errorCode.getCode(), errorCode.getStatus(), errorCode.getDefaultMessage(), request);
    }

    /**
     * Spring MVC request errors (unsupported media type, bad parameter, ...) implement {@link ErrorResponse}
     * and keep their own status; anything else is an internal error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<S3Error> handleException(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse invalidRequest) {
            return error(S3ErrorCode.INVALID_REQUEST.getCode(), invalidRequest.getStatusCode(),
                    invalidRequest.getBody().getDetail(), request);
        }
        log.error("S3 request {} {} failed", request.getMethod(), request.getRequestURI(), e);
        S3ErrorCode errorCode = S3ErrorCode.INTERNAL_ERROR;
        return error(errorCode.getCode(), errorCode.getStatus(), errorCode.getDefaultMessage(), request);
    }

    private ResponseEntity<S3Error> error(String code, HttpStatusCode status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(new S3Error(code, message, request.getRequestURI()));
    }
}
