package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.DepartmentEntity;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.masterportal.dto.DepartmentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final MasterSyncService masterSyncService;

    public List<DepartmentDTO> getAll(String search, String hrmsToken) {
        safeSync(hrmsToken);
        List<DepartmentEntity> entities = (search == null || search.isBlank())
                ? departmentRepository.findAllByOrderByNameAsc()
                : departmentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        return entities.stream()
                .map(DepartmentService::toDto)
                .collect(Collectors.toList());
    }

    public Page<DepartmentDTO> search(String search, int page, int size, String hrmsToken) {
        safeSync(hrmsToken);
        Pageable pageable = PageRequest.of(page, size);
        Page<DepartmentEntity> entities = (search == null || search.isBlank())
                ? departmentRepository.findAllByOrderByNameAsc(pageable)
                : departmentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), pageable);
        return entities.map(DepartmentService::toDto);
    }

    public DepartmentDTO add(DepartmentDTO dto) {
        throw new CommonException("Departments are synced from HRMS. Add them in HRMS, then refresh this list.");
    }

    public DepartmentDTO update(UUID id, DepartmentDTO dto) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        // Name and code come from HRMS; only description is editable in RMS.
        entity.setDescription(trimToNull(dto.getDescription()));
        departmentRepository.save(entity);
        return toDto(entity);
    }

    public void delete(UUID id) {
        throw new CommonException("Departments cannot be deleted in RMS. Deactivate them in HRMS.");
    }

    private void safeSync(String hrmsToken) {
        try {
            masterSyncService.syncDepartments(hrmsToken);
        } catch (Exception ex) {
            log.warn("Department sync failed; returning existing RMS copy: {}", ex.getMessage());
        }
    }

    private static DepartmentDTO toDto(DepartmentEntity e) {
        return DepartmentDTO.builder()
                .id(e.getId())
                .hrmsId(e.getHrmsId())
                .name(e.getName())
                .code(e.getCode())
                .description(e.getDescription())
                .businessUnitHrmsIds(parseCsv(e.getHrmsBusinessUnitIds()))
                .build();
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
