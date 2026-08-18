package com.assessment.urlshortener.config;

import com.assessment.urlshortener.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Applied to the create endpoint (the abuse-prone one — scripted
        // shortening) and the redirect path (the highest-traffic one).
        // Deliberately NOT applied to analytics/delete/update: those are
        // lower-volume, authenticated-in-a-real-deployment operations where
        // rate limiting adds little and the redirect path is the one that
        // actually needs protecting from being hammered.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/shorten", "/{shortCode}")
                .excludePathPatterns(
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**");
    }
}
