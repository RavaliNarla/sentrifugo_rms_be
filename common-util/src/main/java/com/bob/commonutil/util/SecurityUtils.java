package com.bob.commonutil.util;

import com.bob.commonutil.enums.AvailableScreens;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.entity.*;
import com.bob.db.enums.PositionPanelStatus;
import com.bob.db.enums.UserRole;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SecurityUtils {
    @Autowired
    private  UserRepository userRepository;
    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;
    @Autowired
    private JwtUtil jwtUtil;
    public boolean isAdmin() {
        UserEntity user = userRepository.findById(getCurrentUserId()).orElse(null);
        return user != null && DBConstants.USER_ADMIN.equals(user.getRole());
    }
    public String getCurrentUserRole() {
        String header=getClient();
        if(header.equalsIgnoreCase(DBConstants.HEADER_CANDIDATE)){
            return DBConstants.HEADER_CANDIDATE;
        }
        UserEntity user = userRepository.findById(getCurrentUserId()).orElse(null);
        return user != null ? user.getRole() : null;
    }
//    public String getCurrentUserToken() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
//            return jwt.getClaimAsString("sub");
//        }
//        return null;
//    }
//    public UUID getCurrentUserId(){
//        UserEntity user = userRepository.findByOathUserId(getCurrentUserToken()).orElse(null);
//        return user.getId();
//    }
//
    public UUID getCurrentUserId() {
        String header=getClient();
        if(header.equalsIgnoreCase(DBConstants.HEADER_CANDIDATE)){
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if(attrs==null || attrs.getRequest().getCookies()==null){
                return null;
            }
            return jwtUtil.extractUserIdFromCookie(attrs.getRequest());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return  (UUID) auth.getPrincipal();
    }

    public String getCurrentUserName(){
        UserEntity user = userRepository.findById(getCurrentUserId()).
                orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return user.getName();
    }
    public String getClient() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            return null;
        }

        return attrs.getRequest().getHeader(DBConstants.HEADER_XCLIENT);
    }

    public Map<String, Boolean> getPrivileges(String email) {
        UserEntity user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<AvailableScreens, Boolean> screenMap =
                new EnumMap<>(AvailableScreens.class);
        // initialize false
        for (AvailableScreens screen : AvailableScreens.values()) {
            screenMap.put(screen, false);
        }
        Optional<RequisitionApproversEntity> approverEntity = requisitionApproversRepository.findByApproverId(user.getId());
        if(approverEntity.isPresent()){
            screenMap.put(AvailableScreens.VIEW_POSITION,true);
            RequisitionApproversEntity requisitionApproversEntity=approverEntity.get();
            if(requisitionApproversEntity.getApproverRole().equals(RequisitionApproversEntity.ApproverRole.L1)){
                screenMap.put(AvailableScreens.L1_APPROVAL,true);
            }else if(requisitionApproversEntity.getApproverRole().equals(RequisitionApproversEntity.ApproverRole.L2)){
                screenMap.put(AvailableScreens.L2_APPROVAL,true);
            }
        }
        UserRole role = UserRole.fromValue(user.getRole());
        if (role == UserRole.ADMIN) {
            screenMap.put(AvailableScreens.ADMIN, true);

        } else if (role == UserRole.ZONAL_HR) {
            screenMap.put(AvailableScreens.VERIFICATION, true);
        } else if (role == UserRole.RECRUITER) {
            screenMap.put(AvailableScreens.JOB_POSTINGS, true);
            screenMap.put(AvailableScreens.CANDIDATE_POOL, true);
            screenMap.put(AvailableScreens.INTERVIEW_POOL, true);
            screenMap.put(AvailableScreens.SCHEDULE_POOL,true);
            screenMap.put(AvailableScreens.MESSAGES,true);
            screenMap.put(AvailableScreens.EXAM_CONFIGURATION,true);
            screenMap.put(AvailableScreens.OFFER_POOL, true);
            screenMap.put(AvailableScreens.COMPENSATION_POOL, true);
            screenMap.put(AvailableScreens.COMMITTEE_MANAGEMENT, true);
            screenMap.put(AvailableScreens.DASHBOARD,true);
            Set<String> committees = getActiveCommittees(user.getId());
            System.out.println(committees);
            if (committees.contains(DBConstants.INTERVIEW_COMMITTEE_NAME)) {
                screenMap.put(AvailableScreens.INTERVIEW_SCORE, true);
            }
        } else if (role == UserRole.COMMITTEE_MEMBER) {
            Set<String> committees = getActiveCommittees(user.getId());
            System.out.println(committees);
            if (committees.contains(DBConstants.SCREENING_COMMITTEE_NAME)) {
                screenMap.put(AvailableScreens.CANDIDATE_POOL, true);
            }
            if (committees.contains(DBConstants.INTERVIEW_COMMITTEE_NAME)) {
                screenMap.put(AvailableScreens.INTERVIEW_SCORE, true);
            }
            if (committees.contains(DBConstants.COMPENSATION_COMMITTEE_NAME)) {
                screenMap.put(AvailableScreens.COMPENSATION_POOL, true);
            }
        }
        // convert to frontend format
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (AvailableScreens screen : AvailableScreens.values()) {
            result.put(screen.getDisplayName(), screenMap.get(screen));
        }
        return result;
    }

    private Set<String> getActiveCommittees(UUID userId) {
        List<InterviewPanelMembersEntity> interviewPanelMembersEntities = interviewPanelMembersRepository.findAllByPanelMember_Id(userId);
        LocalDate currentDate = LocalDate.now();
//        System.out.println("Members size:"+interviewPanelMembersEntities.size());
        Set<UUID> panelIds = interviewPanelMembersEntities.stream()
                .map(e -> e.getPanel().getId())
                .collect(Collectors.toSet());
//        System.out.println("Panel Ids:"+panelIds);
        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findActivePanels(panelIds,currentDate,List.of(PositionPanelStatus.APPROVED));
        Set<String> committees=positionPanelEntities.stream()
                .map(e -> e.getInterviewPanel().getCommittee().getCommitteeName())
                .collect(Collectors.toSet());
        return committees;
    }


}
