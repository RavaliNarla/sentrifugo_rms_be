package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.EmployeeIdService;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.masterportal.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeIdService employeeIdService;

    public Page<UserDTO> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> entities = (search == null || search.isBlank())
                ? userRepository.findAllByOrderByNameAsc(pageable)
                : userRepository.search(search.trim(), pageable);
        return entities.map(this::toDto);
    }

    @Transactional
    public UserDTO add(UserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new CommonException("Password is required.");
        }
        if (dto.getPassword().length() < 8) {
            throw new CommonException("Password must be at least 8 characters.");
        }
        String email = dto.getEmail().trim();

        // Soft-deleted rows still occupy the unique email index — reactivate instead of insert.
        UserEntity inactiveMatch = userRepository.findIncludingInactiveByEmailIgnoreCase(email).orElse(null);
        if (inactiveMatch != null) {
            if (Boolean.TRUE.equals(inactiveMatch.getIsActive())) {
                throw new CommonException("A user with this email already exists.");
            }
            inactiveMatch.setIsActive(true);
            inactiveMatch.setName(dto.getName().trim());
            inactiveMatch.setRole(dto.getRole());
            inactiveMatch.setEmail(email);
            inactiveMatch.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            if (inactiveMatch.getEmployeeId() == null || inactiveMatch.getEmployeeId().isBlank()
                    || inactiveMatch.getEmployeeId().startsWith("DEL-")) {
                inactiveMatch.setEmployeeId(employeeIdService.nextEmployeeId());
            }
            return toDto(userRepository.save(inactiveMatch));
        }

        UserEntity entity = UserEntity.builder()
                .name(dto.getName().trim())
                .role(dto.getRole())
                .email(email)
                .employeeId(employeeIdService.nextEmployeeId())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();
        return toDto(userRepository.save(entity));
    }

    public UserDTO update(UUID id, UserDTO dto) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        entity.setName(dto.getName().trim());
        entity.setRole(dto.getRole());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8) {
                throw new CommonException("Password must be at least 8 characters.");
            }
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return toDto(userRepository.save(entity));
    }

    /**
     * Soft-delete and free the email/employee_id unique slots so the same values can be reused
     * (reactivation path in {@link #add} still works if we didn't rename — rename is belt-and-suspenders).
     */
    @Transactional
    public void delete(UUID id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private UserDTO toDto(UserEntity entity) {
        return UserDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .role(entity.getRole())
                .email(entity.getEmail())
                .employeeId(entity.getEmployeeId())
                // never expose passwordHash
                .build();
    }
}
