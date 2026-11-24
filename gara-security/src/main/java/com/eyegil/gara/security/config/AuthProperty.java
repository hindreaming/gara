package com.eyegil.gara.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The single Gara account. S3 clients use the username as access key id and the password as secret access key;
 * the admin UI uses the same pair for HTTP Basic.
 */
@ConfigurationProperties("gara.auth")
public record AuthProperty(String username, String password) {

    public AuthProperty {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("gara.auth.username and gara.auth.password must be set (GARA_USERNAME / GARA_PASSWORD)");
        }
    }
}
