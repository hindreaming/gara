package com.eyegil.gara.catalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VolumeProperty.class)
public class CatalogConfig {
}
