package com.sentrifugo.rms.common.config;

import com.sentrifugo.rms.common.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource-server chain for locally issued HS256 JWTs (password login via auth-portal).
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LocalJwtAuthentication localJwtAuthentication;
    private final JwtDecoder jwtDecoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.bearerTokenResolver(request -> {
                                    String path = request.getRequestURI();
                                    // These carry an HRMS access token, not an RMS JWT.
                                    if (path != null && (path.contains("/hrms/access") || path.contains("/hrms/enter"))) {
                                        return null;
                                    }
                                    return new DefaultBearerTokenResolver().resolve(request);
                                })
                                .jwt(jwt -> jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(localJwtAuthentication))
                );
        return http.build();
    }
}
