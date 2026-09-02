package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.EducationQualificationEntity;
import com.sentrifugo.rms.db.repository.EducationQualificationRepository;
import com.sentrifugo.rms.masterportal.dto.NamedMasterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationQualificationService {

    private final EducationQualificationRepository repository;

    public List<NamedMasterDTO> getAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(e -> NamedMasterDTO.builder().id(e.getId()).name(e.getName()).build())
                .collect(Collectors.toList());
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
