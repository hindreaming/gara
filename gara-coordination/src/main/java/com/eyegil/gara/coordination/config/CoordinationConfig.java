package com.eyegil.gara.coordination.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LeaseProperty.class)
public class CoordinationConfig {
}
