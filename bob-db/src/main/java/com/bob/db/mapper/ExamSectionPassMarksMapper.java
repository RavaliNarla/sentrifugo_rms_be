package com.bob.db.mapper;

import com.bob.db.dto.ExamSectionPassMarksDTO;
import com.bob.db.entity.ExamSectionPassMarksEntity;
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
public interface ExamSectionPassMarksMapper {
    ExamSectionPassMarksMapper INSTANCE = Mappers.getMapper(ExamSectionPassMarksMapper.class);

    @Mapping(source = "examConfig.id", target = "examConfigId")
    @Mapping(target = "categoryPassMarks", ignore = true)
    ExamSectionPassMarksDTO toDTO(ExamSectionPassMarksEntity entity);

    @Mapping(source = "examConfigId", target = "examConfig", qualifiedByName = "uuidToExamConfig")
    ExamSectionPassMarksEntity toEntity(ExamSectionPassMarksDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "examConfig", ignore = true)
    @Mapping(target = "sectionNumber", ignore = true)
    void updateEntity(ExamSectionPassMarksDTO dto, @MappingTarget ExamSectionPassMarksEntity entity);

    List<ExamSectionPassMarksDTO> toDTOs(List<ExamSectionPassMarksEntity> entities);
    List<ExamSectionPassMarksEntity> toEntities(List<ExamSectionPassMarksDTO> dtos);

    @Named("uuidToExamConfig")
    default WrittenExamConfigurationEntity uuidToExamConfig(UUID id) {
        if (id == null) return null;
        WrittenExamConfigurationEntity e = new WrittenExamConfigurationEntity();
        e.setId(id);
        return e;
    }
}
