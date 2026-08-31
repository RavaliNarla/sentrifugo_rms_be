package com.bob.db.mapper;

import com.bob.db.dto.CandidateExamSectionMarksDTO;
import com.bob.db.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CandidateExamSectionMarksMapper {
    CandidateExamSectionMarksMapper INSTANCE = Mappers.getMapper(CandidateExamSectionMarksMapper.class);

    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "examConfig.id", target = "examConfigId")
    @Mapping(source = "examSection.id", target = "examSectionId")
    CandidateExamSectionMarksDTO toDTO(CandidateExamSectionMarksEntity entity);

    @Mapping(source = "candidateId", target = "candidate", qualifiedByName = "uuidToCandidate")
    @Mapping(source = "applicationId", target = "application", qualifiedByName = "uuidToApplication")
    @Mapping(source = "examConfigId", target = "examConfig", qualifiedByName = "uuidToExamConfig")
    @Mapping(source = "examSectionId", target = "examSection", qualifiedByName = "uuidToExamSection")
    CandidateExamSectionMarksEntity toEntity(CandidateExamSectionMarksDTO dto);

    List<CandidateExamSectionMarksDTO> toDTOs(List<CandidateExamSectionMarksEntity> entities);
    List<CandidateExamSectionMarksEntity> toEntities(List<CandidateExamSectionMarksDTO> dtos);

    @Named("uuidToCandidate")
    default CandidatesEntity uuidToCandidate(UUID id) {
        if (id == null) return null;
        CandidatesEntity e = new CandidatesEntity();
        e.setId(id);
        return e;
    }

    @Named("uuidToApplication")
    default CandidateApplicationsEntity uuidToApplication(UUID id) {
        if (id == null) return null;
        CandidateApplicationsEntity e = new CandidateApplicationsEntity();
        e.setId(id);
        return e;
    }

    @Named("uuidToExamConfig")
    default WrittenExamConfigurationEntity uuidToExamConfig(UUID id) {
        if (id == null) return null;
        WrittenExamConfigurationEntity e = new WrittenExamConfigurationEntity();
        e.setId(id);
        return e;
    }

    @Named("uuidToExamSection")
    default ExamSectionPassMarksEntity uuidToExamSection(UUID id) {
        if (id == null) return null;
        ExamSectionPassMarksEntity e = new ExamSectionPassMarksEntity();
        e.setId(id);
        return e;
    }
}
