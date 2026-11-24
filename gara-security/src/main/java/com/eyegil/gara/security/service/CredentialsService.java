package com.eyegil.gara.security.service;

import com.eyegil.gara.security.config.AuthProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Checks credentials against the configured account in constant time.
 */
@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final AuthProperty authProperty;

    public boolean matches(String username, String password) {
        return equal(username, authProperty.username()) && equal(password, authProperty.password());
    }

    public boolean isKnownAccessKey(String accessKey) {
        return equal(accessKey, authProperty.username());
    }

    /**
     * The secret that signs requests made with the configured access key.
     */
    String secretKey() {
        return authProperty.password();
    }

    private static boolean equal(String given, String expected) {
        return given != null && MessageDigest.isEqual(
                given.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
