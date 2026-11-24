package com.eyegil.gara.s3.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ETagsTest {

    @Test
    void multipartEtagHashesThePartDigestsAndCountsParts() {
        String a = ETags.of("a".getBytes(StandardCharsets.UTF_8));
        String b = ETags.of("b".getBytes(StandardCharsets.UTF_8));

        assertThat(ETags.ofParts(List.of(a, b))).matches("[0-9a-f]{32}-2");
        assertThat(ETags.ofParts(List.of(a, b))).isNotEqualTo(ETags.ofParts(List.of(b, a)));
    }

    @Test
    void unquotes() {
        assertThat(ETags.unquote("\"abc\"")).isEqualTo("abc");
        assertThat(ETags.unquote("abc")).isEqualTo("abc");
    }
}
