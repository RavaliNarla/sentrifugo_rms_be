//package com.bob.masterdata.utils;
//
//import com.bob.db.entity.UserEntity;
//import com.bob.db.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class SecurityUtils {
//    @Autowired
//    private UserRepository userRepository;
//    public boolean isAdmin() {
//        UserEntity user = userRepository.findByOathUserId(getCurrentUserId()).orElse(null);
//        return user != null && DBConstants.USER_ADMIN.equals(user.getRole());
//    }
//    public String getCurrentUserId() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
//            return jwt.getClaimAsString("sub");
//        }
//        return null;
//    }
//}
//
