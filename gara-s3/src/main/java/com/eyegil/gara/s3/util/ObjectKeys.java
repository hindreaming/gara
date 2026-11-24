package com.eyegil.gara.s3.util;

import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;

import java.nio.charset.StandardCharsets;

public final class ObjectKeys {

    private static final int MAX_KEY_BYTES = 1024;

    private ObjectKeys() {
    }

    /**
     * Turns the captured {@code {*key}} path segment into an object key and rejects keys that cannot be
     * stored as a file path in a git repository.
     */
    public static String fromPath(String capturedPath) {
        String key = capturedPath != null && capturedPath.startsWith("/") ? capturedPath.substring(1) : capturedPath;
        if (key == null || key.isEmpty() || key.endsWith("/")) {
            throw new S3Exception(S3ErrorCode.INVALID_ARGUMENT, "Object key must not be empty or end with '/'.");
        }
        if (key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
            throw new S3Exception(S3ErrorCode.KEY_TOO_LONG);
        }
        if (key.contains("\\")) {
            throw new S3Exception(S3ErrorCode.INVALID_ARGUMENT, "Object key must not contain '\\'.");
        }
        for (String segment : key.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..") || segment.equals(".git")) {
                throw new S3Exception(S3ErrorCode.INVALID_ARGUMENT,
                        "Object key must not contain empty, '.', '..' or '.git' path segments.");
            }
        }
        return key;
    }
}
