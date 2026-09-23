package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.SpecializationEntity;
import com.sentrifugo.rms.db.repository.EducationQualificationRepository;
import com.sentrifugo.rms.db.repository.SpecializationRepository;
import com.sentrifugo.rms.masterportal.dto.SpecializationDTO;
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
public class SpecializationService {

    private final SpecializationRepository repository;
    private final EducationQualificationRepository educationQualificationRepository;

    public Page<SpecializationDTO> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SpecializationEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc(pageable)
                : repository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), pageable);
        return entities.map(this::toDto);
    }

    public List<SpecializationDTO> getByEducation(UUID educationQualificationId) {
        return repository.findByEducationQualificationIdOrGeneral(educationQualificationId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SpecializationDTO add(SpecializationDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.getName())) {
            throw new CommonException("A specialization with this name already exists.");
        }
        SpecializationEntity entity = SpecializationEntity.builder()
                .name(dto.getName())
                .educationQualificationId(dto.getEducationQualificationId())
                .build();
        return toDto(repository.save(entity));
    }

    public SpecializationDTO update(UUID id, SpecializationDTO dto) {
        SpecializationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found"));
        entity.setName(dto.getName());
        entity.setEducationQualificationId(dto.getEducationQualificationId());
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Specialization not found");
        }
        repository.deleteById(id);
    }

    private SpecializationDTO toDto(SpecializationEntity entity) {
        String eduName = entity.getEducationQualificationId() == null ? null :
                educationQualificationRepository.findById(entity.getEducationQualificationId()).map(e -> e.getName()).orElse(null);
        return SpecializationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .educationQualificationId(entity.getEducationQualificationId())
                .educationQualificationName(eduName)
                .build();
    }
}
