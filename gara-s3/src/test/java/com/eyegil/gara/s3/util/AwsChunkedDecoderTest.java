package com.eyegil.gara.s3.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AwsChunkedDecoderTest {

    @Test
    void stripsSignedChunkFraming() {
        String body = "5;chunk-signature=aaaa\r\nhello\r\n6;chunk-signature=bbbb\r\n world\r\n0;chunk-signature=cccc\r\n\r\n";

        assertThat(decode(body)).isEqualTo("hello world");
    }

    @Test
    void stripsUnsignedChunksWithTrailingChecksum() {
        String body = "3\r\nabc\r\n0\r\nx-amz-checksum-crc32:NSRBwg==\r\n\r\n";

        assertThat(decode(body)).isEqualTo("abc");
    }

    private static String decode(String body) {
        return new String(AwsChunkedDecoder.decode(body.getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII);
    }
}
