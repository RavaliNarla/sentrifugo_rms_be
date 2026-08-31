package com.bob.db.mapper;

import com.bob.db.dto.CandidateWrittenExamMarksDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.CandidateWrittenExamMarksEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.entity.WrittenExamConfigurationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateWrittenExamMarksMapper {
    CandidateWrittenExamMarksMapper INSTANCE = Mappers.getMapper(CandidateWrittenExamMarksMapper.class);

    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "position.id", target = "positionId")
    @Mapping(source = "examConfig.id", target = "examConfigId")
    CandidateWrittenExamMarksDTO toDTO(CandidateWrittenExamMarksEntity entity);

    @Mapping(source = "candidateId", target = "candidate", qualifiedByName = "uuidToCandidate")
    @Mapping(source = "applicationId", target = "application", qualifiedByName = "uuidToApplication")
    @Mapping(source = "positionId", target = "position", qualifiedByName = "uuidToPosition")
    @Mapping(source = "examConfigId", target = "examConfig", qualifiedByName = "uuidToExamConfig")
    CandidateWrittenExamMarksEntity toEntity(CandidateWrittenExamMarksDTO dto);

    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "examConfig", ignore = true)
    void updateEntity(CandidateWrittenExamMarksDTO dto, @MappingTarget CandidateWrittenExamMarksEntity entity);

    List<CandidateWrittenExamMarksDTO> toDTOs(List<CandidateWrittenExamMarksEntity> entities);
    List<CandidateWrittenExamMarksEntity> toEntities(List<CandidateWrittenExamMarksDTO> dtos);

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

    @Named("uuidToPosition")
    default JobPositionsEntity uuidToPosition(UUID id) {
        if (id == null) return null;
        JobPositionsEntity e = new JobPositionsEntity();
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
}
