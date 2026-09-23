package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import com.sentrifugo.rms.masterportal.dto.PositionTitleDTO;
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
public class PositionTitleService {

    private final PositionTitleRepository repository;
    private final DepartmentRepository departmentRepository;

    public Page<PositionTitleDTO> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PositionTitleEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc(pageable)
                : repository.search(search.trim(), pageable);
        return entities.map(this::toDto);
    }

    public List<PositionTitleDTO> getByDepartment(UUID departmentId) {
        return repository.findAllByDepartmentIdOrderByNameAsc(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PositionTitleDTO add(PositionTitleDTO dto) {
        if (repository.existsByNameIgnoreCaseAndDepartmentId(dto.getName(), dto.getDepartmentId())) {
            throw new CommonException("A position title with this name already exists in this department.");
        }
        PositionTitleEntity entity = PositionTitleEntity.builder()
                .name(dto.getName())
                .departmentId(dto.getDepartmentId())
                .jobDescription(dto.getJobDescription())
                .minimumExperienceYears(dto.getMinimumExperienceYears())
                .build();
        return toDto(repository.save(entity));
    }

    public PositionTitleDTO update(UUID id, PositionTitleDTO dto) {
        PositionTitleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position title not found"));
        if (!entity.getName().equalsIgnoreCase(dto.getName())
                || !entity.getDepartmentId().equals(dto.getDepartmentId())) {
            if (repository.existsByNameIgnoreCaseAndDepartmentId(dto.getName(), dto.getDepartmentId())) {
                throw new CommonException("A position title with this name already exists in this department.");
            }
        }
        entity.setName(dto.getName());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setJobDescription(dto.getJobDescription());
        entity.setMinimumExperienceYears(dto.getMinimumExperienceYears());
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Position title not found");
        }
        repository.deleteById(id);
    }

    private PositionTitleDTO toDto(PositionTitleEntity entity) {
        String departmentName = entity.getDepartmentId() == null ? null :
                departmentRepository.findById(entity.getDepartmentId()).map(d -> d.getName()).orElse(null);
        return PositionTitleDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .departmentId(entity.getDepartmentId())
                .departmentName(departmentName)
                .jobDescription(entity.getJobDescription())
                .minimumExperienceYears(entity.getMinimumExperienceYears())
                .build();
    }
}
