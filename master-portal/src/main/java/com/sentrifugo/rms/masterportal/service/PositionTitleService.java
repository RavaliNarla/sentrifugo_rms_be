package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import com.sentrifugo.rms.masterportal.dto.PositionTitleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionTitleService {

    private final PositionTitleRepository repository;
    private final DepartmentRepository departmentRepository;
    private final MasterSyncService masterSyncService;

    public Page<PositionTitleDTO> getAll(String search, int page, int size, String hrmsToken) {
        safeSync(hrmsToken);
        Pageable pageable = PageRequest.of(page, size);
        Page<PositionTitleEntity> entities = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc(pageable)
                : repository.search(search.trim(), pageable);
        return entities.map(this::toDto);
    }

    public List<PositionTitleDTO> getByDepartment(UUID departmentId, String hrmsToken) {
        safeSync(hrmsToken);
        return repository.findAvailableForDepartment(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void safeSync(String hrmsToken) {
        try {
            masterSyncService.syncPositionTitles(hrmsToken);
        } catch (Exception ex) {
            log.warn("Position title sync failed; returning existing RMS copy: {}", ex.getMessage());
        }
    }

    public PositionTitleDTO add(PositionTitleDTO dto) {
        throw new CommonException("Position titles are synced from HRMS designations. Add them in HRMS, then refresh this list.");
    }

    public PositionTitleDTO update(UUID id, PositionTitleDTO dto) {
        PositionTitleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position title not found"));
        // Name is owned by HRMS sync. Allow RMS-only: department link, min experience, R&R.
        UUID departmentId = dto.getDepartmentId();
        if (departmentId != null && !departmentRepository.existsById(departmentId)) {
            throw new CommonException("Selected department was not found.");
        }
        entity.setDepartmentId(departmentId);
        entity.setJobDescription(trimToNull(dto.getJobDescription()));
        entity.setMinimumExperienceYears(dto.getMinimumExperienceYears());
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        throw new CommonException("Position titles cannot be deleted in RMS. Deactivate the designation in HRMS.");
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

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
