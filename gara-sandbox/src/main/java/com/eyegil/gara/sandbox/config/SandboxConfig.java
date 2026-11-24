package com.eyegil.gara.sandbox.config;

import com.eyegil.gara.s3.config.ReservedPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Takes {@code /sandbox} away from S3 and serves the page's static files under it. The sandbox API is public on
 * purpose: it can only add files to the sandbox bucket, within the daily limits.
 */
@Configuration
@EnableConfigurationProperties(SandboxProperty.class)
public class SandboxConfig implements WebMvcConfigurer {

    public static final String PATH = "/sandbox";
    public static final String PAGE = PATH + "/index.html";

    /**
     * The sandbox's bucket, which is also the name of its first repository (created public at startup if missing).
     */
    public static final String BUCKET = "gara-sandbox";

    @Bean
    public ReservedPath sandboxPath() {
        return new ReservedPath(PATH);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(PATH + "/*.html", PATH + "/*.css", PATH + "/*.js")
                .addResourceLocations("classpath:/gara-sandbox/");
        registry.addResourceHandler(PATH + "/brand/*").addResourceLocations("classpath:/gara-sandbox/brand/");
    }
}
