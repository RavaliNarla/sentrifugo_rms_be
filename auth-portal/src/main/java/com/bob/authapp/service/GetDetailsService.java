package com.bob.authapp.service;

import com.bob.authapp.model.AllUsersResponse;
import com.bob.authapp.model.GetCandidateResponse;
import com.bob.authapp.model.GetUserResponse;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.entity.UserEntity;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.repository.UserRepository;
import com.bob.db.util.DBConstants;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GetDetailsService {

    @Autowired
    private CandidatesRepository candidateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtils securityUtils;



    public GetCandidateResponse getCandidateByEmail(String email){

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        // Normalize email like Node.js (lowercase + trim)
        email = email.trim().toLowerCase();

        // Fetch candidate
        CandidatesEntity candidate = candidateRepository.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Candidate not found"));

        // Build response
       GetCandidateResponse response =new GetCandidateResponse();
        response.setCandidateId(candidate.getId());
        response.setFullName(candidate.getFullName());
        response.setEmail(candidate.getEmail());
        response.setUsername(candidate.getFullName());


        return response;
    }

    public List<AllUsersResponse> getAllUsers() {
        List<UserEntity> users = userRepository.findAll(
                Sort.by(Sort.Direction.DESC, DBConstants.MASTER_CREATED_DATE)
        );

        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No users found");
        }

        return users.stream()
                .map(u -> new AllUsersResponse(
                        u.getId(),
                        u.getName(),
                        u.getRole(),
                        u.getEmail(),
                        u.getInterviewCenterId()
                ))
                .toList();

    }

    public GetUserResponse getUserByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        // Normalize email
        email = email.trim().toLowerCase();

        // Fetch user by email
        UserEntity user = userRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Compute manager depth (pure JPA)
//        int depth = computeManagerDepth(user.getId());

        int depth = 1;
        GetUserResponse res = new GetUserResponse();
        res.setUserId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        res.setManagerId(user.getManagerId());
        res.setManagerDepth(depth);
        res.setPrivileges(securityUtils.getPrivileges(email));
        return res;
    }


    /** Compute manager depth (JPA recursive parent lookup) */
//    private int computeManagerDepth(UUID userId) {
//        int depth = 0;
//        UUID currentId = userId;
//
//        while (true) {
//            Optional<UserEntity> userOpt = userRepository.findById(currentId);
//
//            if (userOpt.isEmpty()) break;
//
//            UserEntity user = userOpt.get();
//
//            UUID managerId = user.getManagerId();
//            if (managerId == null) break;
//
//            depth++;
//            currentId = managerId;
//        }
//
//        return depth;
//    }

    @Transactional(readOnly = true)
    public Page<AllUsersResponse> searchUsers(String search, Pageable pageable) {
        // Defense in depth: Validate search parameter (should be caught at controller level too)
        if (search == null || search.trim().isEmpty()) {
            throw new IllegalArgumentException("Search parameter is required and cannot be empty");
        }
        
        Specification<UserEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sanitize LIKE special characters to prevent LIKE injection attacks
            String sanitizedSearch = search.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String searchLower = "%" + sanitizedSearch.toLowerCase() + "%";
            Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchLower);
            Predicate emailPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchLower);
            Predicate rolePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), searchLower);
            
            predicates.add(criteriaBuilder.or(namePredicate, emailPredicate, rolePredicate));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserEntity> entitiesPage = userRepository.findAll(spec, pageable);
        return entitiesPage.map(u -> new AllUsersResponse(
                u.getId(),
                u.getName(),
                u.getRole(),
                u.getEmail(),
                u.getInterviewCenterId()
        ));
    }

    public GetUserResponse getUserByToken() {
        UUID userId = securityUtils.getCurrentUserId();
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        GetUserResponse res = new GetUserResponse();
        res.setUserId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        res.setManagerId(user.getManagerId());
        res.setManagerDepth(1);
        res.setPrivileges(securityUtils.getPrivileges(user.getEmail()));
        return res;
    }
}
