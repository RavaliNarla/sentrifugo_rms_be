package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.EducationQualificationEntity;
import com.sentrifugo.rms.db.repository.EducationQualificationRepository;
import com.sentrifugo.rms.masterportal.dto.NamedMasterDTO;
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

    public List<NamedMasterDTO> getAll(String search) {
        List<EducationQualificationEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc()
                : repository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        return entities.stream()
                .map(e -> NamedMasterDTO.builder().id(e.getId()).name(e.getName()).build())
                .collect(Collectors.toList());
    }

    public Page<NamedMasterDTO> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EducationQualificationEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc(pageable)
                : repository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), pageable);
        return entities.map(e -> NamedMasterDTO.builder().id(e.getId()).name(e.getName()).build());
    }

    public NamedMasterDTO add(NamedMasterDTO dto) {
        EducationQualificationEntity entity = repository.save(EducationQualificationEntity.builder().name(dto.getName()).build());
        return NamedMasterDTO.builder().id(entity.getId()).name(entity.getName()).build();
    }

    public NamedMasterDTO update(UUID id, NamedMasterDTO dto) {
        EducationQualificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education qualification not found"));
        entity.setName(dto.getName());
        repository.save(entity);
        return NamedMasterDTO.builder().id(entity.getId()).name(entity.getName()).build();
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Education qualification not found");
        }
        repository.deleteById(id);
    }
}
