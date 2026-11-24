package com.eyegil.gara.s3.util;

import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectKeysTest {

    @Test
    void stripsLeadingSlashOfCapturedPath() {
        assertThat(ObjectKeys.fromPath("/images/logo.png")).isEqualTo("images/logo.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "", "/dir/", "/a//b", "/a/./b", "/a/../b", "/a\\b", "/.git/config", "/x/.git/y"})
    void rejectsKeysThatAreNotValidRepositoryPaths(String capturedPath) {
        assertThatThrownBy(() -> ObjectKeys.fromPath(capturedPath))
                .isInstanceOfSatisfying(S3Exception.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(S3ErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void rejectsKeysLongerThan1024Bytes() {
        assertThatThrownBy(() -> ObjectKeys.fromPath("/" + "a".repeat(1025)))
                .isInstanceOfSatisfying(S3Exception.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(S3ErrorCode.KEY_TOO_LONG));
    }
}
