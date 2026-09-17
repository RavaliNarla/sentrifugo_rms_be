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

import java.time.LocalDate;
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
    private final SpecializationRepository specializationRepository;
    private final CertificationRepository certificationRepository;
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

        validateMandatoryFields(dto);
        if (approvalDoc == null || approvalDoc.isEmpty()) {
            throw new CommonException("Upload Approval Email/Document is required.");
        }

        if (jobPositionRepository.existsByRequisitionIdAndDepartmentIdAndLocationIdAndPositionTitleId(
                dto.getRequisitionId(), dto.getDepartmentId(), dto.getLocationId(), dto.getPositionTitleId())) {
            throw new CommonException("A position with this title already exists for this department and location under this requisition.");
        }

        JobPositionEntity entity = JobPositionEntity.builder()
                .requisitionId(dto.getRequisitionId())
                .status(PositionStatus.NEW)
                .build();
        applyFields(entity, dto);
        entity.setApprovalDocUrl(fileStorageService.store(approvalDoc, APPROVAL_DOCS_FOLDER));

        return toDto(jobPositionRepository.save(entity));
    }

    @Transactional
    public JobPositionDTO update(UUID id, JobPositionDTO dto, MultipartFile approvalDoc) {
        JobPositionEntity entity = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        validateMandatoryFields(dto);
        if ((approvalDoc == null || approvalDoc.isEmpty()) && entity.getApprovalDocUrl() == null) {
            throw new CommonException("Upload Approval Email/Document is required.");
        }

        if (!entity.getDepartmentId().equals(dto.getDepartmentId())
                || !entity.getLocationId().equals(dto.getLocationId())
                || !entity.getPositionTitleId().equals(dto.getPositionTitleId())) {
            if (jobPositionRepository.existsByRequisitionIdAndDepartmentIdAndLocationIdAndPositionTitleId(
                    entity.getRequisitionId(), dto.getDepartmentId(), dto.getLocationId(), dto.getPositionTitleId())) {
                throw new CommonException("A position with this title already exists for this department and location under this requisition.");
            }
        }

        applyFields(entity, dto);
        if (approvalDoc != null && !approvalDoc.isEmpty()) {
            entity.setApprovalDocUrl(fileStorageService.store(approvalDoc, APPROVAL_DOCS_FOLDER));
        }

        return toDto(jobPositionRepository.save(entity));
    }

    private void applyFields(JobPositionEntity entity, JobPositionDTO dto) {
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setLocationId(dto.getLocationId());
        entity.setPositionTitleId(dto.getPositionTitleId());
        entity.setJobDescription(dto.getJobDescription());
        entity.setRolesResponsibilities(dto.getRolesResponsibilities());
        entity.setEducationQualificationId(dto.getEducationQualificationId());
        entity.setSpecializationId(dto.getSpecializationId());
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setCertificationId(dto.getCertificationId());
        entity.setMedicalFitnessRequired(Boolean.TRUE.equals(dto.getMedicalFitnessRequired()));
        entity.setEmploymentType(dto.getEmploymentType() != null ? EmploymentType.valueOf(dto.getEmploymentType()) : EmploymentType.REGULAR);
        // Contractual Period only makes sense for CONTRACT employment - clear it otherwise so no stale value lingers.
        entity.setContractualPeriod(entity.getEmploymentType() == EmploymentType.CONTRACT ? dto.getContractualPeriod() : null);
        entity.setVacancies(dto.getVacancies());
        entity.setApprovedById(dto.getApprovedById());
        entity.setApprovedByOtherText(dto.getApprovedByOtherText());
        entity.setApprovedOn(dto.getApprovedOn());
    }

    private void validateMandatoryFields(JobPositionDTO dto) {
        if (dto.getDepartmentId() == null || dto.getLocationId() == null || dto.getPositionTitleId() == null
                || dto.getJobDescription() == null || dto.getJobDescription().isBlank()) {
            throw new CommonException("Department, Location, Position Title and Job Description are required.");
        }
        if (dto.getEducationQualificationId() == null) {
            throw new CommonException("Education Requirement is required.");
        }
        if (dto.getExperienceYears() == null) {
            throw new CommonException("Experience Required (years) is required.");
        }
        if (dto.getVacancies() == null || dto.getVacancies() < 1) {
            throw new CommonException("Number of Positions to be Hired is required.");
        }
        if (dto.getEmploymentType() == null || dto.getEmploymentType().isBlank()) {
            throw new CommonException("Employment Type is required.");
        }
        if (dto.getApprovedById() == null) {
            throw new CommonException("Approved By is required.");
        }
        if (dto.getApprovedOn() == null) {
            throw new CommonException("Approved On is required.");
        }
        if (dto.getApprovedOn().isAfter(LocalDate.now())) {
            throw new CommonException("Approved On cannot be a future date.");
        }
        ApprovedByRoleEntity role = approvedByRoleRepository.findById(dto.getApprovedById()).orElse(null);
        if (role != null && "others".equalsIgnoreCase(role.getName())
                && (dto.getApprovedByOtherText() == null || dto.getApprovedByOtherText().isBlank())) {
            throw new CommonException("Please enter the approver's name for 'Others'.");
        }
    }

    public void delete(UUID id) {
        JobPositionEntity entity = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
        if (entity.getStatus() != PositionStatus.NEW) {
            throw new CommonException("Only positions in NEW status (not yet submitted/approved) can be deleted.");
        }
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
        Map<UUID, String> specializations = specializationRepository.findAll().stream()
                .collect(Collectors.toMap(SpecializationEntity::getId, SpecializationEntity::getName));
        Map<UUID, String> certifications = certificationRepository.findAll().stream()
                .collect(Collectors.toMap(CertificationEntity::getId, CertificationEntity::getName));
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
                .rolesResponsibilities(entity.getRolesResponsibilities())
                .educationQualificationId(entity.getEducationQualificationId())
                .educationQualificationName(educationQualifications.get(entity.getEducationQualificationId()))
                .specializationId(entity.getSpecializationId())
                .specializationName(specializations.get(entity.getSpecializationId()))
                .experienceYears(entity.getExperienceYears())
                .certificationId(entity.getCertificationId())
                .certificationName(certifications.get(entity.getCertificationId()))
                .medicalFitnessRequired(entity.getMedicalFitnessRequired())
                .contractualPeriod(entity.getContractualPeriod())
                .employmentType(entity.getEmploymentType() != null ? entity.getEmploymentType().name() : null)
                .vacancies(entity.getVacancies())
                .approvalDocUrl(entity.getApprovalDocUrl())
                .approvedById(entity.getApprovedById())
                .approvedByName(approvedByRoles.get(entity.getApprovedById()))
                .approvedByOtherText(entity.getApprovedByOtherText())
                .approvedOn(entity.getApprovedOn())
                .status(entity.getStatus().name())
                .build();
    }
}
