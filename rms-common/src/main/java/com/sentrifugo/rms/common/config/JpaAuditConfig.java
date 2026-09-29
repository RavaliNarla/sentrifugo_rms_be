package com.sentrifugo.rms.common.config;

import com.sentrifugo.rms.common.util.IstTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "istDateTimeProvider")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
                    return Optional.empty();
                }
                Object principal = auth.getPrincipal();
                if (principal instanceof UUID uuid) {
                    return Optional.of(uuid);
                }
                return Optional.empty();
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }

    /** Persist @CreatedDate / @LastModifiedDate as IST wall-clock LocalDateTime. */
    @Bean
    public DateTimeProvider istDateTimeProvider() {
        return () -> Optional.of(IstTime.now());
    }
}
