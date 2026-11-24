package com.eyegil.gara.github.config;

import com.eyegil.integration.github.client.config.annotation.EnableGithubApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableGithubApi
@EnableConfigurationProperties(GithubProperty.class)
public class GithubConfig {
}
