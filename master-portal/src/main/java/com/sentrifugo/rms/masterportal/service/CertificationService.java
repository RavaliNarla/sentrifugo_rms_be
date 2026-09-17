package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.CertificationEntity;
import com.sentrifugo.rms.db.repository.CertificationRepository;
import com.sentrifugo.rms.masterportal.dto.CertificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationRepository repository;

    public List<CertificationDTO> getAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    public CertificationDTO add(CertificationDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.getName())) {
            throw new CommonException("A certification with this name already exists.");
        }
        CertificationEntity entity = repository.save(CertificationEntity.builder().name(dto.getName()).build());
        return toDto(entity);
    }

    public CertificationDTO update(UUID id, CertificationDTO dto) {
        CertificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found"));
        entity.setName(dto.getName());
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Certification not found");
        }
        repository.deleteById(id);
    }

    private CertificationDTO toDto(CertificationEntity entity) {
        return CertificationDTO.builder().id(entity.getId()).name(entity.getName()).build();
    }
}
