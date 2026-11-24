package com.eyegil.gara.s3.util;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;
import java.util.List;

/**
 * S3 entity tags, unquoted.
 */
public final class ETags {

    private static final HexFormat HEX = HexFormat.of();

    private ETags() {
    }

    public static String of(byte[] content) {
        return DigestUtils.md5DigestAsHex(content);
    }

    /**
     * The S3 multipart convention: MD5 of the concatenated binary part MD5s, then "-" and the part count.
     */
    public static String ofParts(List<String> partEtags) {
        ByteArrayOutputStream digests = new ByteArrayOutputStream();
        partEtags.forEach(etag -> digests.writeBytes(HEX.parseHex(etag)));
        return DigestUtils.md5DigestAsHex(digests.toByteArray()) + "-" + partEtags.size();
    }

    /**
     * Strips the quotes clients put around ETags.
     */
    public static String unquote(String etag) {
        return StringUtils.trimTrailingCharacter(StringUtils.trimLeadingCharacter(etag.trim(), '"'), '"');
    }
}
