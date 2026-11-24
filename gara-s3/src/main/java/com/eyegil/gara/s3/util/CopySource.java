package com.eyegil.gara.s3.util;

import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.eyegil.gara.security.util.SigV4Encoding;

/**
 * The source of a CopyObject request, from the {@code x-amz-copy-source} header:
 * {@code [/]bucket/key[?versionId=...]}, percent-encoded.
 */
public record CopySource(String bucket, String key) {

    public static CopySource parse(String header) {
        String value = header.startsWith("/") ? header.substring(1) : header;
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) {
            throw new S3Exception(S3ErrorCode.INVALID_ARGUMENT, "x-amz-copy-source must be bucket/key.");
        }
        return new CopySource(SigV4Encoding.decode(value.substring(0, slash)),
                ObjectKeys.fromPath(SigV4Encoding.decode(value.substring(slash + 1))));
    }
}
