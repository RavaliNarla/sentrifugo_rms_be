package com.bob.commonutil.config;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.entity.UserEntity;
import com.bob.db.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CustomAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public CustomAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String oathUserId = jwt.getSubject();
        Optional<UserEntity> userEntityOptional = userRepository.findByOathUserId(oathUserId);

        if (userEntityOptional.isPresent()) {
            UserEntity userEntity = userEntityOptional.get();
            
            // Create authorities based on user role
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (userEntity.getRole() != null && !userEntity.getRole().isEmpty()) {
                // Add role as authority (e.g., "ADMIN", "RECRUITER", etc.)
                authorities.add(new SimpleGrantedAuthority(userEntity.getRole().toUpperCase()));

            }

            System.out.println("Authorities: " + authorities);
            
            UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(
                    userEntity.getId(), 
                    AppConstants.NOT_AVAILABLE, 
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(user);
            return user;
        }

        return new UsernamePasswordAuthenticationToken(null, AppConstants.NOT_AVAILABLE, Collections.emptyList());
    }
}
