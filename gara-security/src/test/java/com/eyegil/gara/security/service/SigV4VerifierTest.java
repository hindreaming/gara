package com.eyegil.gara.security.service;

import com.eyegil.gara.security.config.AuthProperty;
import com.eyegil.gara.security.dto.SignedRequest;
import com.eyegil.gara.security.exception.SignatureFailure;
import com.eyegil.gara.security.exception.SignatureVerificationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses the "GET Object" example from AWS's Signature Version 4 documentation for S3.
 */
class SigV4VerifierTest {

    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final Instant SIGNED_AT = Instant.parse("2013-05-24T00:00:00Z");
    private static final String AUTHORIZATION = "AWS4-HMAC-SHA256 "
            + "Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,"
            + "SignedHeaders=host;range;x-amz-content-sha256;x-amz-date,"
            + "Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41";

    private final SigV4Verifier verifier = new SigV4Verifier(new CredentialsService(new AuthProperty(ACCESS_KEY, SECRET_KEY)));

    @Test
    void acceptsTheDocumentedExample() {
        assertThatCode(() -> verifier.verify(request(AUTHORIZATION, "/test.txt"), SIGNED_AT)).doesNotThrowAnyException();
    }

    @Test
    void rejectsATamperedPath() {
        assertFailure(() -> verifier.verify(request(AUTHORIZATION, "/other.txt"), SIGNED_AT), SignatureFailure.MISMATCH);
    }

    @Test
    void rejectsAnUnknownAccessKey() {
        String otherKey = AUTHORIZATION.replace(ACCESS_KEY, "SOMEONEELSE");
        assertFailure(() -> verifier.verify(request(otherKey, "/test.txt"), SIGNED_AT), SignatureFailure.UNKNOWN_ACCESS_KEY);
    }

    @Test
    void rejectsOldRequests() {
        assertFailure(() -> verifier.verify(request(AUTHORIZATION, "/test.txt"), SIGNED_AT.plusSeconds(3600)),
                SignatureFailure.CLOCK_SKEW);
    }

    @Test
    void rejectsUnsignedRequests() {
        assertFailure(() -> verifier.verify(request(null, "/test.txt"), SIGNED_AT), SignatureFailure.MISSING);
    }

    private static SignedRequest request(String authorization, String path) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Host", "examplebucket.s3.amazonaws.com");
        headers.put("Range", "bytes=0-9");
        headers.put("x-amz-content-sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        headers.put("x-amz-date", "20130524T000000Z");
        if (authorization != null) {
            headers.put("Authorization", authorization);
        }
        return new SignedRequest("GET", path, null,
                name -> headers.containsKey(name) ? List.of(headers.get(name)) : List.of());
    }

    private static void assertFailure(Runnable verification, SignatureFailure expected) {
        assertThatThrownBy(verification::run)
                .isInstanceOfSatisfying(SignatureVerificationException.class,
                        e -> assertThat(e.getFailure()).isEqualTo(expected));
    }
}
