package com.bob.commonutil.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;


@Builder
@Data
@AllArgsConstructor
public class CustomCandidateUserDetails implements UserDetails {

    private UUID id;
    private String username;
    private String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("CANDIDATE")); // or return roles if you have
    }


}