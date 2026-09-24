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

    public UserDTO add(UserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new CommonException("Password is required.");
        }
        if (dto.getPassword().length() < 8) {
            throw new CommonException("Password must be at least 8 characters.");
        }
        userRepository.findByEmailIgnoreCase(dto.getEmail()).ifPresent(existing -> {
            throw new CommonException("A user with this email already exists.");
        });
        UserEntity entity = UserEntity.builder()
                .name(dto.getName().trim())
                .role(dto.getRole())
                .email(dto.getEmail().trim())
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

    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
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
