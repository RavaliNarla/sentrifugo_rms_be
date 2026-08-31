package com.bob.commonutil.config;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.entity.UserEntity;
import com.bob.db.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
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
public class AzureAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {

    @Autowired
    private UserRepository userRepository;


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("unique_name");

        Optional<UserEntity> userEntityOptional =
                userRepository.findByEmailIgnoreCase(email);

        if(userEntityOptional.isPresent()){
            UserEntity userEntity = userEntityOptional.get();


            // Create authorities based on user role
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (userEntity.getRole() != null && !userEntity.getRole().isEmpty()) {
                // Add role as authority (e.g., "ADMIN", "RECRUITER", etc.)
                authorities.add(new SimpleGrantedAuthority(userEntity.getRole().toUpperCase()));

            }

            return new UsernamePasswordAuthenticationToken(
                    userEntity.getId(),
                    jwt,
                    authorities
            );
        }
        throw new BadCredentialsException("User not found in system");
    }
}
