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
import java.util.Optional;

/**
 * Converts a validated Azure AD JWT into a Spring Security authentication token.
 * The user must already exist in hr.users with a matching email (unique_name claim) -
 * there is no self-provisioning.
 */
@Component
@RequiredArgsConstructor
public class AzureAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("unique_name");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }

        Optional<UserEntity> userEntityOptional = userRepository.findByEmailIgnoreCase(email);

        if (userEntityOptional.isPresent()) {
            UserEntity userEntity = userEntityOptional.get();

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (userEntity.getRole() != null && !userEntity.getRole().isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(userEntity.getRole().toUpperCase()));
            }

            return new UsernamePasswordAuthenticationToken(userEntity.getId(), jwt, authorities);
        }
        throw new BadCredentialsException("User not found in system: " + email);
    }
}
