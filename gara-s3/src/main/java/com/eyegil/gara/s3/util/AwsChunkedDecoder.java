package com.eyegil.gara.s3.util;

import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Removes {@code aws-chunked} framing, which AWS SDKs and the CLI use for uploads
 * ({@code STREAMING-AWS4-HMAC-SHA256-PAYLOAD} and {@code STREAMING-UNSIGNED-PAYLOAD-TRAILER}):
 * <pre>
 * hex-size[;chunk-signature=...]\r\n
 * data\r\n
 * ...
 * 0[;chunk-signature=...]\r\n
 * [trailing headers\r\n]
 * \r\n
 * </pre>
 * Per-chunk signatures and trailing checksums are not verified; the request itself is authenticated by its seed signature.
 */
public final class AwsChunkedDecoder {

    private AwsChunkedDecoder() {
    }

    public static byte[] decode(byte[] body) {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(body.length);
        int position = 0;
        while (true) {
            int lineEnd = indexOfCrlf(body, position);
            String header = new String(body, position, lineEnd - position, StandardCharsets.US_ASCII);
            int size = parseSize(header);
            position = lineEnd + 2;
            if (size == 0) {
                return decoded.toByteArray();
            }
            if (position + size + 2 > body.length) {
                throw malformed();
            }
            decoded.write(body, position, size);
            position += size + 2;
        }
    }

    private static int parseSize(String header) {
        int extension = header.indexOf(';');
        String hex = (extension < 0 ? header : header.substring(0, extension)).trim();
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw malformed();
        }
    }

    private static int indexOfCrlf(byte[] body, int from) {
        for (int i = from; i < body.length - 1; i++) {
            if (body[i] == '\r' && body[i + 1] == '\n') {
                return i;
            }
        }
        throw malformed();
    }

    private static S3Exception malformed() {
        return new S3Exception(S3ErrorCode.INVALID_REQUEST, "Malformed aws-chunked request body.");
    }
}
