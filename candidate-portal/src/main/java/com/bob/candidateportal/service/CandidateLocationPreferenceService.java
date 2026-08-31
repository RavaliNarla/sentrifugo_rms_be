package com.bob.candidateportal.service;

import com.bob.db.dto.CandidateLocationPreferenceDTO;
import com.bob.db.entity.CandidateLocationPreferenceEntity;
import com.bob.db.mapper.CandidateLocationPreferenceMapper;
import com.bob.db.repository.CandidateLocationPreferencesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
public class CandidateLocationPreferenceService {

    @Autowired
    private CandidateLocationPreferencesRepository repository;

    @Autowired
    private CandidateLocationPreferenceMapper mapper;

    public CandidateLocationPreferenceDTO addPreference(CandidateLocationPreferenceDTO dto) {
        CandidateLocationPreferenceEntity entity = mapper.toEntity(dto);
        CandidateLocationPreferenceEntity saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    public List<CandidateLocationPreferenceDTO> getAllPreferences() {
        return mapper.toDTOList(repository.findAll());
    }

    public CandidateLocationPreferenceDTO getPreferenceById(UUID id) {
        CandidateLocationPreferenceEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Preference not found"));
        return mapper.toDTO(entity);
    }

    public CandidateLocationPreferenceDTO updatePreference(UUID id, CandidateLocationPreferenceDTO dto) {
        CandidateLocationPreferenceEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Preference not found"));

        mapper.updateEntityFromDto(dto, entity);

        CandidateLocationPreferenceEntity updated = repository.save(entity);
        return mapper.toDTO(updated);
    }

    public CandidateLocationPreferenceDTO deletePreference(UUID id) {
        CandidateLocationPreferenceEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Preference not found"));

        repository.delete(entity);  // soft delete is automatically applied
        return mapper.toDTO(entity);
    }
}
