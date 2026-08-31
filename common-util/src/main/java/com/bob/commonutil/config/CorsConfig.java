package com.bob.commonutil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "https://localhost:8082",
                "https://localhost:8085",
                "http://localhost:8085",
                "https://bobstage.candidate.sentrifugo.com",
                "https://bobstage.recruitment.sentrifugo.com",
                "https://bobdev.candidate.sentrifugo.com",
                "https://bobdev.recruitment.sentrifugo.com",
                "https://dev.bobjava.sentrifugo.com",
                "https://bob.recruitment.sentrifugo.com",
                "https://bob.candidate.sentrifugo.com",
                "https://test.ccavenue.com",
                "https://secure.ccavenue.com",
                "https://www.ccavenue.com",
                "https://localhost:3000",
                "https://career.mhstech.xyz",
                "https://recruitment.mhstech.xyz",
                "https://consent.digilocker.gov.in"

        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}