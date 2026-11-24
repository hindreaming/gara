package com.eyegil.gara.security.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Percent-encoding as AWS Signature Version 4 defines it: everything except {@code A-Z a-z 0-9 - _ . ~} is encoded
 * with upper-case hex. Clients differ in what they leave unencoded on the wire, so the verifier decodes what it
 * received and re-encodes it this way before comparing.
 */
public final class SigV4Encoding {

    private static final String HEX = "0123456789ABCDEF";

    private SigV4Encoding() {
    }

    public static String encode(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                encoded.append(c);
            } else {
                encoded.append('%').append(HEX.charAt((b >> 4) & 0xF)).append(HEX.charAt(b & 0xF));
            }
        }
        return encoded.toString();
    }

    /**
     * Decodes {@code %XX} sequences only; a literal '+' stays a '+'.
     */
    public static String decode(String value) {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length() && isHex(value, i + 1)) {
                decoded.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                decoded.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }
        return decoded.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHex(String value, int at) {
        return Character.digit(value.charAt(at), 16) >= 0 && Character.digit(value.charAt(at + 1), 16) >= 0;
    }
}
