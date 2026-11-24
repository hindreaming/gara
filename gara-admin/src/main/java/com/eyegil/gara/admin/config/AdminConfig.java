package com.eyegil.gara.admin.config;

import com.eyegil.gara.admin.web.AdminBasicAuthFilter;
import com.eyegil.gara.s3.config.ReservedPath;
import com.eyegil.gara.security.service.CredentialsService;
import com.eyegil.gara.security.service.LoginAttemptGuard;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the static admin UI (from the gara-ui module) under {@code /admin/ui/} and protects everything under
 * {@code /admin} and {@code /actuator} with HTTP Basic.
 * <p>
 * The UI's resource mapping runs before controller mappings, because S3's catch-all {@code /{bucket}/{*key}} would
 * otherwise claim {@code /admin/ui/...}. That only works with Spring Boot's default {@code /**} static mapping
 * switched off ({@code spring.web.resources.add-mappings=false}), which the application does.
 */
@Configuration
public class AdminConfig implements WebMvcConfigurer {

    public static final String UI_PATH = "/admin/ui/";

    @Bean
    public FilterRegistrationBean<AdminBasicAuthFilter> adminBasicAuthFilter(CredentialsService credentialsService,
                                                                             LoginAttemptGuard loginAttemptGuard) {
        FilterRegistrationBean<AdminBasicAuthFilter> registration =
                new FilterRegistrationBean<>(new AdminBasicAuthFilter(credentialsService, loginAttemptGuard));
        registration.addUrlPatterns("/admin", "/admin/*", "/actuator/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    public ReservedPath adminPath() {
        return new ReservedPath("/admin");
    }

    @Bean
    public ReservedPath actuatorPath() {
        return new ReservedPath("/actuator");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(UI_PATH + "**").addResourceLocations("classpath:/gara-ui/");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
