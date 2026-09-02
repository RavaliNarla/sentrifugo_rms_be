package com.sentrifugo.rms.masterportal.controller;

import com.sentrifugo.rms.common.dto.ApiResponse;
import com.sentrifugo.rms.db.entity.ApprovedByRoleEntity;
import com.sentrifugo.rms.db.entity.OfferTemplateEntity;
import com.sentrifugo.rms.db.entity.StateEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.UserRole;
import com.sentrifugo.rms.db.repository.ApprovedByRoleRepository;
import com.sentrifugo.rms.db.repository.OfferTemplateRepository;
import com.sentrifugo.rms.db.repository.StateRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.masterportal.dto.NamedMasterDTO;
import com.sentrifugo.rms.masterportal.dto.OfferTemplateDTO;
import com.sentrifugo.rms.masterportal.dto.UserDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only dropdown data used across Job Posting / Committee Management screens.
 */
@Tag(name = "Master Dropdown Data")
@RestController
@RequestMapping("${master.api.base.path}/master-dd-data")
@RequiredArgsConstructor
public class MasterDropdownController {

    private final StateRepository stateRepository;
    private final ApprovedByRoleRepository approvedByRoleRepository;
    private final OfferTemplateRepository offerTemplateRepository;
    private final UserRepository userRepository;

    @GetMapping("/get/states")
    public ResponseEntity<ApiResponse<List<NamedMasterDTO>>> getStates() {
        List<NamedMasterDTO> states = stateRepository.findAllByOrderByNameAsc().stream()
                .map(e -> NamedMasterDTO.builder().id(e.getId()).name(e.getName()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(states, "States fetched successfully"));
    }

    @GetMapping("/get/approved-by-roles")
    public ResponseEntity<ApiResponse<List<NamedMasterDTO>>> getApprovedByRoles() {
        List<NamedMasterDTO> roles = approvedByRoleRepository.findAllByOrderByNameAsc().stream()
                .map(e -> NamedMasterDTO.builder().id(e.getId()).name(e.getName()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(roles, "Approved-by roles fetched successfully"));
    }

    @GetMapping("/get/offer-templates")
    public ResponseEntity<ApiResponse<List<OfferTemplateDTO>>> getOfferTemplates() {
        List<OfferTemplateDTO> templates = offerTemplateRepository.findAllByOrderByNameAsc().stream()
                .map(e -> OfferTemplateDTO.builder().id(e.getId()).name(e.getName()).fileName(e.getFileName()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(templates, "Offer templates fetched successfully"));
    }

    @GetMapping("/get/panel-members")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getPanelMembers() {
        List<UserDTO> members = userRepository
                .findByRoleIn(List.of(UserRole.RECRUITER.getValue(), UserRole.COMMITTEE_MEMBER.getValue()))
                .stream()
                .map(u -> UserDTO.builder().id(u.getId()).name(u.getName()).role(u.getRole()).email(u.getEmail()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(members, "Panel members fetched successfully"));
    }
}
