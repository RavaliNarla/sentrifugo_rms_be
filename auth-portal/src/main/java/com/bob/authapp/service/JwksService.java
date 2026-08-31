package com.bob.authapp.service;

import com.bob.commonutil.exception.CommonException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;

import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;


import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
public class JwksService {

    @Value("${auth0.domain}")
    private String domain;

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @PostConstruct
    public void init() {
        try {
            String jwksUrl = domain + "/.well-known/jwks.json";

            JWKSource<SecurityContext> keySource =
                    new RemoteJWKSet<>(new URL(jwksUrl));

            jwtProcessor = new DefaultJWTProcessor<>();

            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);

            jwtProcessor.setJWSKeySelector(keySelector);

//            System.out.println("JWKS initialized: " + jwksUrl);

        } catch (Exception e) {

            throw new CommonException("Failed to initialize JWT processor");
        }
    }

    public JWTClaimsSet validate(String token) {
        try {
            return jwtProcessor.process(token, null);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid or expired token", e);
        }
    }
}
