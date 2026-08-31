package com.bob.commonutil.util;

import com.bob.commonutil.model.CustomCandidateUserDetails;
import com.bob.db.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<UUID> getCurrentAuditor() {
        try{
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            // 🔹 Candidate case
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomCandidateUserDetails customUser) {
                return Optional.of(customUser.getId());

            }

            // 🔹 Recruiter case
            if (authentication instanceof UsernamePasswordAuthenticationToken token) {
               return Optional.of(UUID.fromString(token.getPrincipal().toString()));
            }



            return Optional.of(new UUID(0,0));
        }catch (Exception e){
            return Optional.of(new UUID(0,0));
        }
    }
}