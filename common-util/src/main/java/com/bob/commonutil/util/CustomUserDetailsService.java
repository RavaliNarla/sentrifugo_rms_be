package com.bob.commonutil.util;

import com.bob.commonutil.model.CustomCandidateUserDetails;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.repository.CandidatesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CandidatesRepository repo;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CandidatesEntity u = repo.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return CustomCandidateUserDetails.builder()
                .id(u.getId())
                .username(u.getEmail())
                .password(u.getPasswordHash()).build();
    }
}

