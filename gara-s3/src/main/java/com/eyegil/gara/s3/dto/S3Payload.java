package com.eyegil.gara.s3.dto;

/**
 * A request body after S3 transfer decoding ({@code aws-chunked}) and integrity checks.
 * Declare it as a controller parameter to receive it.
 */
public record S3Payload(byte[] bytes) {
}
