package com.eyegil.gara.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthProperty.class, LockoutProperty.class})
public class SecurityConfig {
}
