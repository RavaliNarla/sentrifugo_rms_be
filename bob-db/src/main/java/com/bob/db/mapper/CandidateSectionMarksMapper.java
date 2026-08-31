package com.bob.db.mapper;

import com.bob.db.dto.CandidateSectionMarksDTO;
import com.bob.db.entity.CandidateSectionMarksEntity;
import com.bob.db.entity.CandidateWrittenExamMarksEntity;
import com.bob.db.entity.ExamSectionPassMarksEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateSectionMarksMapper {
    CandidateSectionMarksMapper INSTANCE = Mappers.getMapper(CandidateSectionMarksMapper.class);

    @Mapping(source = "candidateExam.id", target = "candidateExamId")
    @Mapping(source = "examSection.id", target = "examSectionId")
    CandidateSectionMarksDTO toDTO(CandidateSectionMarksEntity entity);

    @Mapping(source = "candidateExamId", target = "candidateExam", qualifiedByName = "uuidToCandidateExam")
    @Mapping(source = "examSectionId", target = "examSection", qualifiedByName = "uuidToExamSection")
    CandidateSectionMarksEntity toEntity(CandidateSectionMarksDTO dto);

    @Mapping(target = "candidateExam", ignore = true)
    @Mapping(target = "examSection", ignore = true)
    void updateEntity(CandidateSectionMarksDTO dto, @MappingTarget CandidateSectionMarksEntity entity);

    List<CandidateSectionMarksDTO> toDTOs(List<CandidateSectionMarksEntity> entities);
    List<CandidateSectionMarksEntity> toEntities(List<CandidateSectionMarksDTO> dtos);

    @Named("uuidToCandidateExam")
    default CandidateWrittenExamMarksEntity uuidToCandidateExam(UUID id) {
        if (id == null) return null;
        CandidateWrittenExamMarksEntity e = new CandidateWrittenExamMarksEntity();
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
