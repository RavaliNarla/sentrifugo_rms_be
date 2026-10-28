package com.sentrifugo.rms.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Browser origins allowed to call RMS APIs (HRMS FE access-check + RMS FE).
 * Override with {@code app.cors.allowed-origin-patterns} / {@code APP_CORS_ALLOWED_ORIGIN_PATTERNS}
 * (comma-separated). Port {@code *} covers local 3000/5300 and HRMS 5173/5174/5274.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://172.16.2.111:*,https://rms.sentrifugo.com,https://bobdev.recruitment.sentrifugo.com}")
            String allowedOriginPatterns
    ) {
        List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
