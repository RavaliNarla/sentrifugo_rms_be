package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.repository.RequisitionApproverRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.masterportal.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String APPROVER_NONE = "NONE";
    public static final String APPROVER_L1 = "L1";
    public static final String APPROVER_L2 = "L2";

    private final UserRepository userRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserDTO> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> entities = (search == null || search.isBlank())
                ? userRepository.findAllByOrderByNameAsc(pageable)
                : userRepository.search(search.trim(), pageable);
        return entities.map(this::toDto);
    }

    @Transactional
    public UserDTO add(UserDTO dto) {
        String employeeId = dto.getEmployeeId() == null ? "" : dto.getEmployeeId().trim();
        if (employeeId.isBlank()) {
            throw new CommonException("Employee Id from HRMS is required.");
        }
        if (employeeId.length() > 32) {
            throw new CommonException("Employee Id must be 32 characters or fewer.");
        }
        String email = dto.getEmail().trim();
        userRepository.findByEmployeeIdIgnoreCase(employeeId).ifPresent(existing -> {
            throw new CommonException("A user with this Employee Id already exists.");
        });

        UserEntity saved;
        UserEntity inactiveMatch = userRepository.findIncludingInactiveByEmailIgnoreCase(email).orElse(null);
        if (inactiveMatch != null) {
            if (Boolean.TRUE.equals(inactiveMatch.getIsActive())) {
                throw new CommonException("A user with this email already exists.");
            }
            inactiveMatch.setIsActive(true);
            inactiveMatch.setName(dto.getName().trim());
            inactiveMatch.setRole(dto.getRole());
            inactiveMatch.setEmail(email);
            inactiveMatch.setEmployeeId(employeeId);
            inactiveMatch.setPasswordHash(null);
            saved = userRepository.save(inactiveMatch);
        } else {
            UserEntity entity = UserEntity.builder()
                    .name(dto.getName().trim())
                    .role(dto.getRole())
                    .email(email)
                    .employeeId(employeeId)
                    .build();
            saved = userRepository.save(entity);
        }

        applyApproverAccess(saved.getId(), normalizeApproverAccess(dto.getApproverAccess(), APPROVER_NONE),
                dto.getReplacementUserId(), true);
        return toDto(saved);
    }

    @Transactional
    public UserDTO update(UUID id, UserDTO dto) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureAdminRemains(entity, dto.getRole());
        entity.setName(dto.getName().trim());
        entity.setRole(dto.getRole());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8) {
                throw new CommonException("Password must be at least 8 characters.");
            }
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        UserEntity saved = userRepository.save(entity);

        if (dto.getApproverAccess() != null) {
            applyApproverAccess(saved.getId(), normalizeApproverAccess(dto.getApproverAccess(), APPROVER_NONE),
                    dto.getReplacementUserId(), true);
        }
        return toDto(saved);
    }

    /**
     * Soft-delete and free the email/employee_id unique slots so the same values can be reused
     * (reactivation path in {@link #add} still works if we didn't rename — rename is belt-and-suspenders).
     */
    @Transactional
    public void delete(UUID id, UUID replacementUserId) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureAdminRemains(entity, null);

        Optional<RequisitionApproverEntity> current = requisitionApproverRepository.findByApproverId(id);
        if (current.isPresent()) {
            transferApproverSlot(current.get(), id, replacementUserId);
        }

        String originalEmail = entity.getEmail();
        String originalEmp = entity.getEmployeeId();
        entity.setEmail("deleted." + id + "." + originalEmail);
        if (originalEmp != null && !originalEmp.isBlank()) {
            String suffix = id.toString().replace("-", "");
            String freed = "DEL-" + suffix.substring(0, Math.min(8, suffix.length())) + "-" + originalEmp;
            entity.setEmployeeId(freed.length() > 32 ? freed.substring(0, 32) : freed);
        }
        userRepository.save(entity);
        userRepository.delete(entity);
    }

    /** newRole null means delete. Blocks removing or demoting the last Admin. */
    private void ensureAdminRemains(UserEntity entity, String newRole) {
        if (entity.getRole() == null || !"Admin".equalsIgnoreCase(entity.getRole())) {
            return;
        }
        boolean stayingAdmin = newRole != null && "Admin".equalsIgnoreCase(newRole);
        if (stayingAdmin) {
            return;
        }
        long admins = userRepository.findByRoleIn(List.of("Admin")).size();
        if (admins <= 1) {
            throw new CommonException("At least one Admin must remain.");
        }
    }

    /**
     * @param required when true, {@code desired} must be NONE/L1/L2; used for add/update when client sent a value.
     */
    private void applyApproverAccess(UUID userId, String desired, UUID replacementUserId, boolean required) {
        if (!required && desired == null) {
            return;
        }
        String target = desired == null ? APPROVER_NONE : desired;
        Optional<RequisitionApproverEntity> currentOpt = requisitionApproverRepository.findByApproverId(userId);
        String current = currentOpt.map(row -> row.getApproverRole().name()).orElse(APPROVER_NONE);

        if (current.equals(target)) {
            return;
        }

        // Direct L1 <-> L2 swap would leave a vacant slot mid-save.
        if (!APPROVER_NONE.equals(current) && !APPROVER_NONE.equals(target) && !current.equals(target)) {
            throw new CommonException(
                    "Clear Approver access to None first (and choose a replacement), then assign the other level.");
        }

        if (APPROVER_NONE.equals(target)) {
            transferApproverSlot(currentOpt.orElseThrow(), userId, replacementUserId);
            return;
        }

        ApproverRole role = ApproverRole.valueOf(target);
        assertSlotAvailable(role, userId);
        if (currentOpt.isPresent()) {
            // Should not happen after checks above; clear defensively.
            requisitionApproverRepository.delete(currentOpt.get());
        }
        requisitionApproverRepository.save(RequisitionApproverEntity.builder()
                .approverId(userId)
                .approverRole(role)
                .build());
    }

    private void transferApproverSlot(RequisitionApproverEntity current, UUID fromUserId, UUID replacementUserId) {
        if (replacementUserId == null) {
            throw new CommonException(
                    "Select a replacement " + labelFor(current.getApproverRole())
                            + " before removing this Approver access.");
        }
        if (replacementUserId.equals(fromUserId)) {
            throw new CommonException("Replacement must be a different user.");
        }
        UserEntity replacement = userRepository.findById(replacementUserId)
                .orElseThrow(() -> new CommonException("Replacement user was not found or is inactive."));
        if (requisitionApproverRepository.findByApproverId(replacement.getId()).isPresent()) {
            throw new CommonException(
                    replacement.getName() + " is already an Approver. Choose someone who is not L1 or L2.");
        }
        ApproverRole role = current.getApproverRole();
        // Soft-delete current holder's row, then give the slot to the replacement.
        requisitionApproverRepository.delete(current);
        requisitionApproverRepository.save(RequisitionApproverEntity.builder()
                .approverId(replacement.getId())
                .approverRole(role)
                .build());
    }

    private void assertSlotAvailable(ApproverRole role, UUID forUserId) {
        List<RequisitionApproverEntity> holders = requisitionApproverRepository.findByApproverRole(role);
        for (RequisitionApproverEntity holder : holders) {
            if (!holder.getApproverId().equals(forUserId)) {
                String name = userRepository.findById(holder.getApproverId())
                        .map(UserEntity::getName)
                        .orElse("another user");
                throw new CommonException(
                        labelFor(role) + " is already assigned to " + name + ".");
            }
        }
    }

    private static String labelFor(ApproverRole role) {
        return role == ApproverRole.L1 ? "L1 Approver" : "L2 Approver";
    }

    private static String normalizeApproverAccess(String raw, String defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("NONE".equals(normalized) || "L1".equals(normalized) || "L2".equals(normalized)) {
            return normalized;
        }
        if ("L1 APPROVER".equals(normalized) || "L1_APPROVER".equals(normalized)) {
            return APPROVER_L1;
        }
        if ("L2 APPROVER".equals(normalized) || "L2_APPROVER".equals(normalized)) {
            return APPROVER_L2;
        }
        throw new CommonException("Approver access must be None, L1 Approver, or L2 Approver.");
    }

    private UserDTO toDto(UserEntity entity) {
        String access = requisitionApproverRepository.findByApproverId(entity.getId())
                .map(row -> row.getApproverRole().name())
                .orElse(APPROVER_NONE);
        return UserDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .role(entity.getRole())
                .email(entity.getEmail())
                .employeeId(entity.getEmployeeId())
                .approverAccess(access)
                .build();
    }
}
