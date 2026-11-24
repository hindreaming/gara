package com.eyegil.gara.s3.dto;

/**
 * An object to send back: its metadata for the headers and its content.
 */
public record S3StoredObject(S3ObjectMetadata metadata, byte[] content) {
}
