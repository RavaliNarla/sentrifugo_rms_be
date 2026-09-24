package com.sentrifugo.rms.common.config;

import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converts a locally issued JWT (subject = hr.users.id) into a Spring Security principal.
 */
@Component
@RequiredArgsConstructor
public class LocalJwtAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token subject");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found for token"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().toUpperCase()));
        }

        return new UsernamePasswordAuthenticationToken(user.getId(), jwt, authorities);
    }
}
