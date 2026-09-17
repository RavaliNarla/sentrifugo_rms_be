package com.sentrifugo.rms.masterportal.config;

import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Idempotent one-time seeder for master data the client shared in the
 * requirements doc (Section 1 Location list, Section 4 Approved-By list,
 * Section 1/5 Department list, plus a starter Specialization list per this
 * session's ask). Only inserts rows that don't already exist by name - never
 * touches or deletes existing data a tester/recruiter may have already added.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterDataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final StateRepository stateRepository;
    private final ApprovedByRoleRepository approvedByRoleRepository;
    private final SpecializationRepository specializationRepository;
    private final EducationQualificationRepository educationQualificationRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final CertificationRepository certificationRepository;

    private static final List<String> DEPARTMENTS = List.of(
            "MARKETING", "PROCESS & QC", "MECHANICAL", "HR & ADMIN", "MINES & AUTOMOBILE",
            "ELECTRICAL", "INSTRUMENTATION", "COMMERCIAL & PACKING", "ACCOUNTS & FINANCE",
            "PURCHASE & STORES", "CAPTIVE POWER PLANT", "WHRS & WTP", "OPERATIONS", "CIVIL",
            "ENVIRONMENT & SAFETY", "IT & EDP", "PROJECTS & DEVELOPMENT", "PLANT HEAD OFFICE",
            "SECRETARIAL", "SENIOR LEADERSHIP"
    );

    private static final List<String> LOCATIONS = List.of(
            "HEAD OFFICE", "MATTAMPALLY", "JEERABAD", "GUDIPADU", "DACHEPALLI",
            "BAYYAVARAM", "JAJPUR", "GBC", "LIS"
    );

    private static final List<String> APPROVED_BY_ROLES = List.of(
            "JMD", "Group President", "Plant Head", "Dept Head/HOD",
            "Unit HR/Plant HR", "Corporate HR", "GM, HR", "Others"
    );

    /** name, keyword to match against an existing Education Qualification name (nullable link if no match). */
    private static final String[][] SPECIALIZATIONS = {
            {"Computer Science and Engineering", "tech"},
            {"Mechanical Engineering", "tech"},
            {"Electrical and Electronics Engineering", "tech"},
            {"Electronics and Communication Engineering", "tech"},
            {"Civil Engineering", "tech"},
            {"Chemical Engineering", "tech"},
            {"Human Resources", "mba"},
            {"Finance", "mba"},
            {"Marketing", "mba"},
            {"General", null},
    };

    /**
     * Sample Position Master rows (name → department). Used both to create missing
     * titles and to backfill department_id on older orphan rows so Add Position's
     * department-filtered dropdown is never empty for these depts.
     */
    private static final List<String> CERTIFICATIONS = List.of(
            "NCCBM Certified Quality Controller",
            "Cement Manufacturing Technology (NCB)",
            "ISO 9001:2015 Lead Auditor",
            "ISO 14001 Environmental Management Auditor",
            "Concrete Technology Certification",
            "Kiln Operations & Pyroprocessing Certification",
            "NEBOSH Occupational Health & Safety",
            "Six Sigma Green Belt",
            "Green Building / LEED AP",
            "First Aid & Fire Safety Certification",
            "Crane & Heavy Equipment Operator License",
            "SAP Plant Maintenance (PM) Certification"
    );

    private static final String[][] POSITION_TITLES = {
            {"Accounts Executive", "ACCOUNTS & FINANCE"},
            {"Administrative Officer", "HR & ADMIN"},
            {"HR Executive", "HR & ADMIN"},
            {"Plant Supervisor", "OPERATIONS"},
            {"Project Manager", "PROJECTS & DEVELOPMENT"},
            {"Quality Analyst", "PROCESS & QC"},
            {"Sales Executive", "MARKETING"},
            {"Software Engineer", "IT & EDP"},
            {"Senior Software Engineer", "IT & EDP"},
    };

    @Override
    public void run(String... args) {
        seedDepartments();
        seedLocations();
        seedApprovedByRoles();
        seedSpecializations();
        seedCertifications();
        seedPositionTitles();
    }

    private void seedDepartments() {
        for (String name : DEPARTMENTS) {
            if (!departmentRepository.existsByNameIgnoreCase(name)) {
                departmentRepository.save(DepartmentEntity.builder().name(name).build());
            }
        }
    }

    private void seedLocations() {
        List<String> existing = locationRepository.findAllByOrderByNameAsc().stream()
                .map(LocationEntity::getName).map(String::toUpperCase).toList();
        List<String> toAdd = LOCATIONS.stream().filter(l -> !existing.contains(l.toUpperCase())).toList();
        if (toAdd.isEmpty()) {
            return;
        }
        UUID stateId = stateRepository.findAll().stream().findFirst().map(StateEntity::getId).orElse(null);
        if (stateId == null) {
            log.warn("No State master data found - skipping Location seed until at least one State exists.");
            return;
        }
        for (String name : toAdd) {
            locationRepository.save(LocationEntity.builder().name(name).stateId(stateId).build());
        }
    }

    private void seedApprovedByRoles() {
        List<String> existing = approvedByRoleRepository.findAllByOrderByNameAsc().stream()
                .map(ApprovedByRoleEntity::getName).map(String::toLowerCase).toList();
        for (String name : APPROVED_BY_ROLES) {
            if (!existing.contains(name.toLowerCase())) {
                approvedByRoleRepository.save(ApprovedByRoleEntity.builder().name(name).build());
            }
        }
    }

    private void seedSpecializations() {
        List<EducationQualificationEntity> educations = educationQualificationRepository.findAll();
        for (String[] pair : SPECIALIZATIONS) {
            String name = pair[0];
            String keyword = pair[1];
            if (specializationRepository.existsByNameIgnoreCase(name)) {
                continue;
            }
            UUID eduId = null;
            if (keyword != null) {
                eduId = educations.stream()
                        .filter(e -> e.getName() != null && e.getName().toLowerCase().contains(keyword))
                        .map(EducationQualificationEntity::getId)
                        .findFirst()
                        .orElse(null);
            }
            specializationRepository.save(SpecializationEntity.builder().name(name).educationQualificationId(eduId).build());
        }
    }

    private void seedCertifications() {
        for (String name : CERTIFICATIONS) {
            // Soft-deleted rows still hold the unique name; check including inactive.
            if (!certificationRepository.existsByNameIgnoreCaseIncludingInactive(name)) {
                certificationRepository.save(CertificationEntity.builder().name(name).build());
            }
        }
    }

    private void seedPositionTitles() {
        List<DepartmentEntity> departments = departmentRepository.findAll();
        for (String[] pair : POSITION_TITLES) {
            String titleName = pair[0];
            String deptName = pair[1];
            UUID deptId = departments.stream()
                    .filter(d -> d.getName() != null && d.getName().equalsIgnoreCase(deptName))
                    .map(DepartmentEntity::getId)
                    .findFirst()
                    .orElse(null);
            if (deptId == null) {
                log.warn("Skipping position title '{}' - department '{}' not found", titleName, deptName);
                continue;
            }

            PositionTitleEntity existing = positionTitleRepository.findAllByOrderByNameAsc().stream()
                    .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(titleName))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                if (existing.getDepartmentId() == null) {
                    existing.setDepartmentId(deptId);
                    positionTitleRepository.save(existing);
                    log.info("Linked orphan position title '{}' to department '{}'", titleName, deptName);
                }
                continue;
            }

            if (positionTitleRepository.existsByNameIgnoreCaseAndDepartmentId(titleName, deptId)) {
                continue;
            }
            positionTitleRepository.save(PositionTitleEntity.builder()
                    .name(titleName)
                    .departmentId(deptId)
                    .jobDescription("Role responsibilities as defined by the hiring department.")
                    .minimumExperienceYears(1)
                    .build());
        }
    }
}
