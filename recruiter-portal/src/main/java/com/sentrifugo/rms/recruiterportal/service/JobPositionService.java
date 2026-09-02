package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.EmploymentType;
import com.sentrifugo.rms.db.enums.PositionStatus;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.JobPositionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final EducationQualificationRepository educationQualificationRepository;
    private final ApprovedByRoleRepository approvedByRoleRepository;
    private final FileStorageService fileStorageService;

    private static final String APPROVAL_DOCS_FOLDER = "approval-docs";

    @Transactional
    public JobPositionDTO create(JobPositionDTO dto, MultipartFile approvalDoc) {
        JobRequisitionEntity requisition = jobRequisitionRepository.findById(dto.getRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
        if (requisition.getStatus() != RequisitionStatus.NEW
                && requisition.getStatus() != RequisitionStatus.L1_REJECTED
                && requisition.getStatus() != RequisitionStatus.L2_REJECTED) {
            throw new CommonException("Positions can only be added while the requisition is editable (NEW/REJECTED).");
        }

        JobPositionEntity entity = JobPositionEntity.builder()
                .requisitionId(dto.getRequisitionId())
                .departmentId(dto.getDepartmentId())
                .locationId(dto.getLocationId())
                .positionTitleId(dto.getPositionTitleId())
                .jobDescription(dto.getJobDescription())
                .educationQualificationId(dto.getEducationQualificationId())
                .experienceYears(dto.getExperienceYears())
                .employmentType(dto.getEmploymentType() != null ? EmploymentType.valueOf(dto.getEmploymentType()) : EmploymentType.REGULAR)
                .vacancies(dto.getVacancies() != null ? dto.getVacancies() : 1)
                .approvedById(dto.getApprovedById())
                .approvedOn(dto.getApprovedOn())
                .status(PositionStatus.NEW)
                .build();

        if (approvalDoc != null && !approvalDoc.isEmpty()) {
            entity.setApprovalDocUrl(fileStorageService.store(approvalDoc, APPROVAL_DOCS_FOLDER));
        }

        return toDto(jobPositionRepository.save(entity));
    }

    @Transactional
    public JobPositionDTO update(UUID id, JobPositionDTO dto, MultipartFile approvalDoc) {
        JobPositionEntity entity = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        entity.setDepartmentId(dto.getDepartmentId());
        entity.setLocationId(dto.getLocationId());
        entity.setPositionTitleId(dto.getPositionTitleId());
        entity.setJobDescription(dto.getJobDescription());
        entity.setEducationQualificationId(dto.getEducationQualificationId());
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setEmploymentType(dto.getEmploymentType() != null ? EmploymentType.valueOf(dto.getEmploymentType()) : entity.getEmploymentType());
        entity.setVacancies(dto.getVacancies());
        entity.setApprovedById(dto.getApprovedById());
        entity.setApprovedOn(dto.getApprovedOn());

        if (approvalDoc != null && !approvalDoc.isEmpty()) {
            entity.setApprovalDocUrl(fileStorageService.store(approvalDoc, APPROVAL_DOCS_FOLDER));
        }

        return toDto(jobPositionRepository.save(entity));
    }

    public void delete(UUID id) {
        JobPositionEntity entity = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
        jobPositionRepository.delete(entity);
    }

    public List<JobPositionDTO> getByRequisitionId(UUID requisitionId) {
        return jobPositionRepository.findByRequisitionId(requisitionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<JobPositionDTO> getActiveByRequisitionId(UUID requisitionId) {
        return jobPositionRepository.findByRequisitionIdAndStatus(requisitionId, PositionStatus.ACTIVE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public JobPositionDTO getById(UUID id) {
        return toDto(jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found")));
    }

    private JobPositionDTO toDto(JobPositionEntity entity) {
        Map<UUID, String> departments = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, DepartmentEntity::getName));
        Map<UUID, String> locations = locationRepository.findAll().stream()
                .collect(Collectors.toMap(LocationEntity::getId, LocationEntity::getName));
        Map<UUID, String> positionTitles = positionTitleRepository.findAll().stream()
                .collect(Collectors.toMap(PositionTitleEntity::getId, PositionTitleEntity::getName));
        Map<UUID, String> educationQualifications = educationQualificationRepository.findAll().stream()
                .collect(Collectors.toMap(EducationQualificationEntity::getId, EducationQualificationEntity::getName));
        Map<UUID, String> approvedByRoles = approvedByRoleRepository.findAll().stream()
                .collect(Collectors.toMap(ApprovedByRoleEntity::getId, ApprovedByRoleEntity::getName));

        return JobPositionDTO.builder()
                .id(entity.getId())
                .requisitionId(entity.getRequisitionId())
                .departmentId(entity.getDepartmentId())
                .departmentName(departments.get(entity.getDepartmentId()))
                .locationId(entity.getLocationId())
                .locationName(locations.get(entity.getLocationId()))
                .positionTitleId(entity.getPositionTitleId())
                .positionTitleName(positionTitles.get(entity.getPositionTitleId()))
                .jobDescription(entity.getJobDescription())
                .educationQualificationId(entity.getEducationQualificationId())
                .educationQualificationName(educationQualifications.get(entity.getEducationQualificationId()))
                .experienceYears(entity.getExperienceYears())
                .employmentType(entity.getEmploymentType() != null ? entity.getEmploymentType().name() : null)
                .vacancies(entity.getVacancies())
                .approvalDocUrl(entity.getApprovalDocUrl())
                .approvedById(entity.getApprovedById())
                .approvedByName(approvedByRoles.get(entity.getApprovedById()))
                .approvedOn(entity.getApprovedOn())
                .status(entity.getStatus().name())
                .build();
    }
}
