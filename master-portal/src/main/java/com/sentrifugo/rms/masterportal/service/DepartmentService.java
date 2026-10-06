package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.util.MasterCodeUtil;
import com.sentrifugo.rms.db.entity.DepartmentEntity;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.masterportal.dto.DepartmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentDTO> getAll(String search) {
        List<DepartmentEntity> entities = (search == null || search.isBlank())
                ? departmentRepository.findAllByOrderByNameAsc()
                : departmentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        return entities.stream()
                .map(DepartmentService::toDto)
                .collect(Collectors.toList());
    }

    public Page<DepartmentDTO> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DepartmentEntity> entities = (search == null || search.isBlank())
                ? departmentRepository.findAllByOrderByNameAsc(pageable)
                : departmentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), pageable);
        return entities.map(DepartmentService::toDto);
    }

    public DepartmentDTO add(DepartmentDTO dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new CommonException("A department with this name already exists.");
        }
        String code = MasterCodeUtil.normalizeRequired(dto.getCode(), "Department");
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new CommonException("A department with code '" + code + "' already exists.");
        }
        DepartmentEntity entity = departmentRepository.save(DepartmentEntity.builder()
                .name(dto.getName().trim())
                .code(code)
                .description(trimToNull(dto.getDescription()))
                .build());
        return toDto(entity);
    }

    public DepartmentDTO update(UUID id, DepartmentDTO dto) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        String code = MasterCodeUtil.normalizeRequired(dto.getCode(), "Department");
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new CommonException("A department with code '" + code + "' already exists.");
        }
        entity.setName(dto.getName().trim());
        entity.setCode(code);
        entity.setDescription(trimToNull(dto.getDescription()));
        departmentRepository.save(entity);
        return toDto(entity);
    }

    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found");
        }
        departmentRepository.deleteById(id);
    }

    private static DepartmentDTO toDto(DepartmentEntity e) {
        return DepartmentDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .description(e.getDescription())
                .build();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
