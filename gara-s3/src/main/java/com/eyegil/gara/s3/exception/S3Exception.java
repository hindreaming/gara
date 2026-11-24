package com.eyegil.gara.s3.exception;

import lombok.Getter;

@Getter
public class S3Exception extends RuntimeException {

    private final S3ErrorCode errorCode;

    public S3Exception(S3ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public S3Exception(S3ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
