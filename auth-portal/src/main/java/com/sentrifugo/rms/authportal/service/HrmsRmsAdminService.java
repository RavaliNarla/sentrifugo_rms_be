package com.sentrifugo.rms.authportal.service;

import com.sentrifugo.rms.authportal.dto.RmsAdminActiveRequest;
import com.sentrifugo.rms.authportal.dto.RmsAdminCreateRequest;
import com.sentrifugo.rms.authportal.dto.RmsAdminDTO;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.repository.RequisitionApproverRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HrmsRmsAdminService {

    private static final String ROLE_ADMIN = "Admin";

    private final HrmsLinkService hrmsLinkService;
    private final UserRepository userRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;

    public List<RmsAdminDTO> list(String authorization) {
        hrmsLinkService.requireValidSession(authorization);
        return userRepository.findAdminsIncludingInactive().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RmsAdminDTO add(String authorization, RmsAdminCreateRequest request) {
        hrmsLinkService.requireValidSession(authorization);

        String employeeId = request.getEmployeeId() == null ? "" : request.getEmployeeId().trim();
        if (employeeId.isBlank()) {
            throw new CommonException("Employee Id from HRMS is required.");
        }
        if (employeeId.length() > 32) {
            throw new CommonException("Employee Id must be 32 characters or fewer.");
        }
        String email = request.getEmail().trim();
        String name = request.getName().trim();

        UserEntity byEmail = userRepository.findIncludingInactiveByEmailIgnoreCase(email).orElse(null);
        if (byEmail != null) {
            if (Boolean.TRUE.equals(byEmail.getIsActive())) {
                throw new CommonException("A user with this email already exists.");
            }
            assertEmployeeIdAvailable(employeeId, byEmail.getId());
            byEmail.setIsActive(true);
            byEmail.setName(name);
            byEmail.setRole(ROLE_ADMIN);
            byEmail.setEmail(email);
            byEmail.setEmployeeId(employeeId);
            byEmail.setPasswordHash(null);
            return toDto(userRepository.save(byEmail));
        }

        UserEntity byEmp = userRepository.findIncludingInactiveByEmployeeIdIgnoreCase(employeeId).orElse(null);
        if (byEmp != null) {
            if (Boolean.TRUE.equals(byEmp.getIsActive())) {
                throw new CommonException("A user with this Employee Id already exists.");
            }
            byEmp.setIsActive(true);
            byEmp.setName(name);
            byEmp.setRole(ROLE_ADMIN);
            byEmp.setEmail(email);
            byEmp.setEmployeeId(employeeId);
            byEmp.setPasswordHash(null);
            return toDto(userRepository.save(byEmp));
        }

        UserEntity entity = UserEntity.builder()
                .name(name)
                .role(ROLE_ADMIN)
                .email(email)
                .employeeId(employeeId)
                .build();
        return toDto(userRepository.save(entity));
    }

    @Transactional
    public RmsAdminDTO setActive(String authorization, UUID id, RmsAdminActiveRequest request) {
        hrmsLinkService.requireValidSession(authorization);
        UserEntity entity = userRepository.findIncludingInactiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (entity.getRole() == null || !ROLE_ADMIN.equalsIgnoreCase(entity.getRole())) {
            throw new CommonException("Only RMS Admin users can be toggled here.");
        }
        if (!Boolean.TRUE.equals(request.getActive())) {
            requisitionApproverRepository.findByApproverId(id).ifPresent(row -> {
                String level = row.getApproverRole() == ApproverRole.L1 ? "L1" : "L2";
                throw new CommonException(
                        "This user is currently an " + level
                                + " Approver in Recruitment. Ask an RMS Admin to assign another Approver "
                                + "under Users before deactivating them here.");
            });
        }
        entity.setIsActive(Boolean.TRUE.equals(request.getActive()));
        return toDto(userRepository.save(entity));
    }

    private void assertEmployeeIdAvailable(String employeeId, UUID selfId) {
        userRepository.findIncludingInactiveByEmployeeIdIgnoreCase(employeeId).ifPresent(other -> {
            if (!other.getId().equals(selfId)) {
                throw new CommonException("A user with this Employee Id already exists.");
            }
        });
    }

    private RmsAdminDTO toDto(UserEntity entity) {
        return RmsAdminDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .employeeId(entity.getEmployeeId())
                .isActive(entity.getIsActive())
                .build();
    }
}
