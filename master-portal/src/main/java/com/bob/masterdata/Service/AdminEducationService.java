package com.bob.masterdata.Service;

import com.bob.commonutil.exception.ManualValidationException;
import com.bob.db.dto.EducationGroupsDTO;
import com.bob.db.dto.SpecializationMasterDTO;
import com.bob.db.entity.EducationGroupsEntity;
import com.bob.db.entity.EducationQualificationsEntity;
import com.bob.db.entity.QualificationGroupMappingEntity;
import com.bob.db.entity.SpecializationMasterEntity;
import com.bob.db.mapper.EducationGroupsMapper;
import com.bob.db.mapper.EducationQualificationsMapper;
import com.bob.db.mapper.QualificationGroupMappingMapper;
import com.bob.db.mapper.SpecializationMasterMapper;
import com.bob.db.repository.*;
import com.bob.masterdata.Model.AdminEducationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class AdminEducationService {

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private SpecializationMasterRepository specializationMasterRepository;

    @Autowired
    private EducationQualificationsMapper educationQualificationsMapper;

    @Autowired
    private SpecializationMasterMapper specializationMasterMapper;


    @Autowired
    private QualificationGroupMappingRepository qualificationGroupMappingRepository;

    @Autowired
    private QualificationGroupMappingMapper qualificationGroupMappingMapper;

    @Autowired
    private EducationGroupsRepository educationGroupsRepository;

    @Autowired
    private EducationGroupsMapper educationGroupsMapper;

    @Transactional
    public void saveEducationDetails(AdminEducationModel request){
        EducationQualificationsEntity qualification;
        //save or update qualification
        if (request.getQualification().getId() == null) {
            qualification = educationQualificationsMapper.toEntity(request.getQualification());
        } else {
            qualification = educationQualificationsRepository.findById(request.getQualification().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Qualification not found"));
            qualification.setQualificationCode(request.getQualification().getQualificationCode());
            qualification.setQualificationName(request.getQualification().getQualificationName());
            qualification.setLevelId(request.getQualification().getLevelId());
            qualification.setDisplayOrder(request.getQualification().getDisplayOrder());
        }
        qualification = educationQualificationsRepository.save(qualification);
        UUID qualificationId = qualification.getId();
        List<SpecializationMasterEntity> existingSpecializations = specializationMasterRepository.findAllByEducationQualificationsId(qualificationId);
        Map<UUID, SpecializationMasterEntity> existingSpecializationMap = existingSpecializations.stream()
                        .collect(Collectors.toMap(
                                SpecializationMasterEntity::getId,
                                Function.identity()
                        ));

        List<UUID> retainedIds = new ArrayList<>();
        for (AdminEducationModel.SpecializationGroupingModel model : request.getSpecializations()) {
            SpecializationMasterDTO dto = model.getSpecialization();
            SpecializationMasterEntity entity;
            if (dto == null) {
                continue;
            }
            if(dto.getId()==null && dto.getSpecializationName()==null && dto.getSpecializationCode()==null){
                continue;
            }
            if (dto.getId() == null) {
                entity = specializationMasterMapper.toEntity(dto);
                entity.setEducationQualificationsId(qualificationId);

            } else {
                entity = existingSpecializationMap.get(dto.getId());
                if (entity == null) {
                    throw new IllegalArgumentException("Specialization not found");
                }
                entity.setSpecializationCode(dto.getSpecializationCode());
                entity.setSpecializationName(dto.getSpecializationName());
            }
            entity = specializationMasterRepository.save(entity);
            retainedIds.add(entity.getId());
            dto.setId(entity.getId());
        }

        List<UUID> idsToDelete = existingSpecializations.stream()
                .map(SpecializationMasterEntity::getId)
                .filter(id -> !retainedIds.contains(id))
                .toList();

        if (!idsToDelete.isEmpty()) {
            specializationMasterRepository.deleteAllById(idsToDelete);
        }

        qualificationGroupMappingRepository.deleteByEducationQualificationId(qualificationId);

        List<QualificationGroupMappingEntity> mappings = new ArrayList<>();

        for (AdminEducationModel.SpecializationGroupingModel model : request.getSpecializations()) {

            QualificationGroupMappingEntity mapping = QualificationGroupMappingEntity.builder()
                            .educationQualificationId(qualificationId)
                            .specializationId(
                                    model.getSpecialization() == null
                                            ? null
                                            : model.getSpecialization().getId()
                            )
                            .educationGroupId(model.getGroup().getId())
                            .build();

            mappings.add(mapping);
        }

        qualificationGroupMappingRepository.saveAll(mappings);
    }

//    @Transactional
//    public List<AdminEducationModel> getAllEducationDetails(List<UUID> educationLevelIds){
//        if(educationLevelIds == null || educationLevelIds.isEmpty()){
//            throw new IllegalArgumentException("Education Levels not found");
//        }
//
//        List<EducationQualificationsEntity> educationQualifications = educationQualificationsRepository.findAllByLevelIdIn(educationLevelIds);
//        List<UUID> educationQualificationIds = educationQualifications.stream()
//        .map(EducationQualificationsEntity::getId)
//                .toList();
//
//        //Map with qualId
//        Map<UUID, List<QualificationGroupMappingEntity>> qualificationGroupMapping = qualificationGroupMappingRepository.findByEducationQualificationIdIn(educationQualificationIds)
//                        .stream()
//                        .collect(Collectors.groupingBy(
//                                QualificationGroupMappingEntity::getEducationQualificationId
//                        ));
//
//        //Get the groupIds and specIds
//        List<UUID> groupIds = qualificationGroupMapping.values().stream()
//                .flatMap(List::stream)
//                .map(QualificationGroupMappingEntity::getEducationGroupId)
//                .distinct()
//                .toList();
//
//        List<UUID> specializationIds=qualificationGroupMapping.values().stream()
//                .flatMap(List::stream)
//                .map(QualificationGroupMappingEntity::getSpecializationId)
//                .distinct()
//                .toList();
//
//        // Create maps with specialization and group entities for easy access
//        Map<UUID, SpecializationMasterEntity> specializationMap = specializationMasterRepository.findAllById(specializationIds)
//                .stream().collect(Collectors.toMap(SpecializationMasterEntity::getId, Function.identity()));
//
//        Map<UUID, EducationGroupsEntity> groupMap = educationGroupsRepository.findAllById(groupIds)
//                        .stream().collect(Collectors.toMap(EducationGroupsEntity::getId, Function.identity()));
//
//        List<AdminEducationModel> adminEducationModels = educationQualifications.stream().map(qualification -> {
//            List<AdminEducationModel.SpecializationGroupingModel> specializationGroupingModels = qualificationGroupMapping.getOrDefault(qualification.getId(), Collections.emptyList())
//                    .stream()
//                    .map(mapping -> {
//                        SpecializationMasterEntity specialization = specializationMap.get(mapping.getSpecializationId());
//                        EducationGroupsEntity group = groupMap.get(mapping.getEducationGroupId());
//                        return AdminEducationModel.SpecializationGroupingModel.builder()
//                                .specialization(specializationMasterMapper.toDTO(specialization))
//                                .group(educationGroupsMapper.toDTO(group))
//                                .build();
//                    })
//                    .toList();
//
//            return AdminEducationModel.builder()
//                    .qualification(educationQualificationsMapper.toDTO(qualification))
//                    .specializations(specializationGroupingModels)
//                    .build();
//        }).toList();
//
//        return adminEducationModels;
//
//    }

    public record QualificationMappingKey(
            UUID qualificationId,
            UUID specializationId
    ) {
    }

    public List<AdminEducationModel> getAllEducationDetails(List<UUID> educationLevelIds) {

        if (educationLevelIds == null || educationLevelIds.isEmpty()) {
            throw new IllegalArgumentException("Education Levels not found");
        }

        List<EducationQualificationsEntity> educationQualifications =
                educationQualificationsRepository.findAllByLevelIdIn(educationLevelIds);

        List<UUID> educationQualificationIds = educationQualifications.stream()
                .map(EducationQualificationsEntity::getId)
                .toList();

        // Fetch all specializations
        List<SpecializationMasterEntity> specializations =
                specializationMasterRepository.findAllByEducationQualificationsIdIn(educationQualificationIds);

        // Fetch qualification-group mappings
        List<QualificationGroupMappingEntity> qualificationGroupMappings =
                qualificationGroupMappingRepository.findByEducationQualificationIdIn(educationQualificationIds);

        // Fetch all groups
        List<UUID> groupIds = qualificationGroupMappings.stream()
                .map(QualificationGroupMappingEntity::getEducationGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, EducationGroupsDTO> groupMap =
                educationGroupsRepository.findAllById(groupIds)
                        .stream()
                        .collect(Collectors.toMap(
                                EducationGroupsEntity::getId,
                                educationGroupsMapper::toDTO
                        ));

        // qualificationId -> List<Specialization>
        Map<UUID, List<SpecializationMasterEntity>> specializationMap =
                specializations.stream()
                        .collect(Collectors.groupingBy(
                                SpecializationMasterEntity::getEducationQualificationsId
                        ));

        // (qualificationId,specializationId) -> Mapping
        Map<QualificationMappingKey, QualificationGroupMappingEntity> mappingMap =
                qualificationGroupMappings.stream()
                        .collect(Collectors.toMap(
                                mapping -> new QualificationMappingKey(
                                        mapping.getEducationQualificationId(),
                                        mapping.getSpecializationId()
                                ),
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));

        return educationQualifications.stream()
                .map(qualification -> {
                    List<SpecializationMasterEntity> qualificationSpecializations = specializationMap.getOrDefault(
                                    qualification.getId(),
                                    Collections.emptyList());
                    List<AdminEducationModel.SpecializationGroupingModel> specializationModels;
                    // Qualification without specializations (BCA, MBA, etc.)
                    if (qualificationSpecializations.isEmpty()) {
                        QualificationGroupMappingEntity mapping =
                                mappingMap.get(new QualificationMappingKey(
                                        qualification.getId(),
                                        null));
                        specializationModels = List.of(AdminEducationModel.SpecializationGroupingModel.builder()
                                        .specialization(null)
                                        .group(mapping == null ? null : groupMap.get(mapping.getEducationGroupId()))
                                        .build());

                    } else {
                        specializationModels = qualificationSpecializations.stream()
                                .map(spec -> {
                                    QualificationGroupMappingEntity mapping = mappingMap.get(new QualificationMappingKey(
                                                            qualification.getId(),
                                                            spec.getId()));
                                    return AdminEducationModel.SpecializationGroupingModel.builder()
                                            .specialization(specializationMasterMapper.toDTO(spec))
                                            .group(mapping == null ? null : groupMap.get(mapping.getEducationGroupId()))
                                            .build();
                                })
                                .toList();
                    }

                    return AdminEducationModel.builder()
                            .qualification(
                                    educationQualificationsMapper.toDTO(qualification))
                            .specializations(specializationModels)
                            .build();
                })
                .toList();
    }

}
