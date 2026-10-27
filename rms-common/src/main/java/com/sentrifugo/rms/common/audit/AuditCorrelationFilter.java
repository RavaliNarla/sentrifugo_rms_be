package com.sentrifugo.rms.common.audit;

import com.sentrifugo.rms.db.audit.AuditCorrelation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a {@code correlation_id} for the request (from {@code X-Correlation-Id} or a new UUID)
 * and exposes it on the response for clients/logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuditCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        UUID correlationId = parseOrCreate(request.getHeader(HEADER));
        AuditCorrelation.set(correlationId);
        response.setHeader(HEADER, correlationId.toString());
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditCorrelation.clear();
        }
    }

    private static UUID parseOrCreate(String header) {
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return UUID.randomUUID();
    }
}
