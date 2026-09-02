package com.sentrifugo.rms.common.util;

import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.enums.UserRole;
import com.sentrifugo.rms.db.repository.RequisitionApproverRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;

    public UUID getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (UUID) principal;
    }

    public UserEntity getCurrentUser() {
        return userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in hr.users"));
    }

    public String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /**
     * Screen-level privileges shown/enforced on the frontend, mirroring the AvailableScreens
     * pattern from the reference project but trimmed to what this app actually has.
     */
    public Map<String, Boolean> getPrivileges(UUID userId) {
        Map<String, Boolean> privileges = new LinkedHashMap<>();
        privileges.put("Admin", false);
        privileges.put("JobPostings", false);
        privileges.put("CandidatePool", false);
        privileges.put("InterviewPool", false);
        privileges.put("CompensationPool", false);
        privileges.put("OfferPool", false);
        privileges.put("CommitteeManagement", false);
        privileges.put("Interview", false);
        privileges.put("L1Approval", false);
        privileges.put("L2Approval", false);
        privileges.put("Dashboard", false);

        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return privileges;
        }

        UserRole role = UserRole.fromValue(user.getRole());
        if (role == UserRole.ADMIN) {
            privileges.put("Admin", true);
            privileges.put("Dashboard", true);
        } else if (role == UserRole.RECRUITER) {
            privileges.put("JobPostings", true);
            privileges.put("CandidatePool", true);
            privileges.put("InterviewPool", true);
            privileges.put("CompensationPool", true);
            privileges.put("OfferPool", true);
            privileges.put("CommitteeManagement", true);
            privileges.put("Dashboard", true);
        } else if (role == UserRole.COMMITTEE_MEMBER) {
            privileges.put("Interview", true);
        }

        Optional<RequisitionApproverEntity> approver = requisitionApproverRepository.findByApproverId(userId);
        if (approver.isPresent()) {
            privileges.put("JobPostings", true);
            if (approver.get().getApproverRole() == ApproverRole.L1) {
                privileges.put("L1Approval", true);
            } else if (approver.get().getApproverRole() == ApproverRole.L2) {
                privileges.put("L2Approval", true);
            }
        }

        return privileges;
    }
}
