package com.bob.commonutil.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(30, Duration.ofDays(1))
                        .build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Apply only to login API
        if (uri.endsWith("/api/v1/recruiter/job-requisitions/create")) {

            String clientIp = request.getRemoteAddr();

            Bucket bucket = buckets.computeIfAbsent(
                    clientIp,
                    k -> createBucket());

            if (!bucket.tryConsume(1)) {
                response.sendError(429, "Rate limit exceeded");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}