package com.bob.commonutil.util;

import com.bob.db.util.DBConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Logger logger = LoggerFactory.getLogger(CookieBearerTokenResolver.class);

    @Override
    public String resolve(HttpServletRequest request) {
        String authHeader = request.getHeader(DBConstants.HEADER_AUTHORIZATION);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        //logger.info("Authorization header: {}", authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}