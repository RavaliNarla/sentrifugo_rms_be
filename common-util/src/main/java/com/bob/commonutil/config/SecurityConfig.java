package com.bob.commonutil.config;

import com.bob.commonutil.util.*;
import com.bob.db.util.DBConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

//    @Value("${auth0.jwt.jwk-set-uri}")
//    private String jwkSetUri;
//
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;


    @Autowired
    private  CookieBearerTokenResolver tokenResolver;
    @Autowired
     private AzureAuthentication azureAuthentication;
    @Autowired
    private  CustomAuthenticationConverter customAuthenticationConverter;


    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;


    @Bean
    @Order(1)
    public SecurityFilterChain candidateChain(HttpSecurity http) throws Exception {
        var matcher = new RequestHeaderRequestMatcher(DBConstants.HEADER_XCLIENT, DBConstants.HEADER_CANDIDATE);
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(cookie -> cookie
                .path("/")
                .sameSite("None")
                .secure(true)
        );
        http
                .securityMatcher(matcher)
                .cors(cors -> {}) // Enable CORS with default settings
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repo)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                );
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain azureADChain(HttpSecurity http) throws Exception {

        var matcher = new RequestHeaderRequestMatcher(DBConstants.HEADER_XCLIENT, DBConstants.HEADER_AZURE_AD);
        http.securityMatcher(matcher)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.bearerTokenResolver(tokenResolver)
                                .jwt(jwt -> jwt.decoder(azureJwtDecoder())
                                        .jwtAuthenticationConverter(azureAuthentication))
                );
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().denyAll()
                );

        return http.build();
    }


    @Bean
    public JwtDecoder azureJwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }


}
