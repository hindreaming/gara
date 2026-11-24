package com.eyegil.gara.s3.config;

import com.eyegil.gara.s3.util.S3Xml;
import com.eyegil.gara.s3.web.ReservedPaths;
import com.eyegil.gara.s3.web.S3AuthenticationFilter;
import com.eyegil.gara.s3.web.S3PayloadArgumentResolver;
import com.eyegil.gara.s3.web.UploadConcurrencyFilter;
import com.eyegil.gara.security.service.LoginAttemptGuard;
import com.eyegil.gara.security.service.SigV4Verifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties({ObjectProperty.class, S3Property.class})
public class S3Config implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<S3AuthenticationFilter> s3AuthenticationFilter(SigV4Verifier sigV4Verifier,
                                                                                 LoginAttemptGuard loginAttemptGuard,
                                                                                 ReservedPaths reservedPaths,
                                                                                 S3Xml s3Xml) {
        FilterRegistrationBean<S3AuthenticationFilter> registration = new FilterRegistrationBean<>(
                new S3AuthenticationFilter(sigV4Verifier, loginAttemptGuard, reservedPaths, s3Xml));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * Runs after authentication, so unauthenticated requests never take an upload slot.
     */
    @Bean
    public FilterRegistrationBean<UploadConcurrencyFilter> uploadConcurrencyFilter(S3Property s3Property, ReservedPaths reservedPaths,
                                                                                   S3Xml s3Xml) {
        FilterRegistrationBean<UploadConcurrencyFilter> registration =
                new FilterRegistrationBean<>(new UploadConcurrencyFilter(s3Property, reservedPaths, s3Xml));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new S3PayloadArgumentResolver());
    }
}
