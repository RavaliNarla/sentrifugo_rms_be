package com.bob.authapp.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Date;
import java.util.List;

@Component
public class JwtValidator {

    @Value("${auth0.domain}")
    private String domain;

    public void validateAccessToken(String token) throws Exception {

        URL jwksUrl = new URL(domain + "/.well-known/jwks.json");

        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(jwksUrl);

        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

        jwtProcessor.setJWSKeySelector(keySelector);

        // Custom claims verifier to avoid strict matching
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {

            // 1. Validate issuer
            String expectedIssuer = domain + "/";
            if (!expectedIssuer.equals(claims.getIssuer())) {
                throw new BadJWTException("Invalid issuer");
            }

            // 2. Validate audience contains your API audience
            String requiredAudience = domain + "/api/v2/";
            List<String> audList = claims.getAudience();
            if (audList == null || !audList.contains(requiredAudience)) {
                throw new BadJWTException("Invalid audience");
            }

            // 3. Validate expiration
            Date now = new Date();
            if (claims.getExpirationTime() == null || now.after(claims.getExpirationTime())) {
                throw new BadJWTException("Token expired");
            }
        });

        // Parse & validate
        SignedJWT jwt = SignedJWT.parse(token);
        jwtProcessor.process(jwt, null);
    }

}
