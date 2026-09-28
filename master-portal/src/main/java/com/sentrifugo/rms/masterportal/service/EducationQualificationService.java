package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.EducationQualificationEntity;
import com.sentrifugo.rms.db.repository.EducationQualificationRepository;
import com.sentrifugo.rms.masterportal.dto.EducationQualificationDTO;
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
public class EducationQualificationService {

    private final EducationQualificationRepository repository;

    public List<EducationQualificationDTO> getAll(String search) {
        List<EducationQualificationEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc()
                : repository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        return entities.stream()
                .map(e -> toDto(e))
                .collect(Collectors.toList());
    }

    public Page<EducationQualificationDTO> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EducationQualificationEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc(pageable)
                : repository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), pageable);
        return entities.map(e -> toDto(e));
    }

    public EducationQualificationDTO add(EducationQualificationDTO dto) {
        EducationQualificationEntity entity = repository.save(EducationQualificationEntity.builder().name(dto.getName()).description(trimToNull(dto.getDescription())).build());
        return toDto(entity);
    }

    public EducationQualificationDTO update(UUID id, EducationQualificationDTO dto) {
        EducationQualificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education qualification not found"));
        entity.setName(dto.getName());
        entity.setDescription(trimToNull(dto.getDescription()));
        repository.save(entity);
        return toDto(entity);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Education qualification not found");
        }
        repository.deleteById(id);
    }

    private static EducationQualificationDTO toDto(EducationQualificationEntity e) {
        return EducationQualificationDTO.builder().id(e.getId()).name(e.getName()).description(e.getDescription()).build();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
