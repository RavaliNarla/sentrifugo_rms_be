package com.bob.db.mapper;

import com.bob.db.dto.ExamSectionCategoryPassMarksDTO;
import com.bob.db.entity.ExamSectionCategoryPassMarksEntity;
import com.bob.db.entity.ExamSectionPassMarksEntity;
import com.bob.db.entity.ReservationCategoriesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExamSectionCategoryPassMarksMapper {
    ExamSectionCategoryPassMarksMapper INSTANCE = Mappers.getMapper(ExamSectionCategoryPassMarksMapper.class);

    @Mapping(source = "examSection.id", target = "examSectionId")
    @Mapping(source = "category.id", target = "categoryId")
    ExamSectionCategoryPassMarksDTO toDTO(ExamSectionCategoryPassMarksEntity entity);

    @Mapping(source = "examSectionId", target = "examSection", qualifiedByName = "uuidToExamSection")
    @Mapping(source = "categoryId", target = "category", qualifiedByName = "uuidToCategory")
    ExamSectionCategoryPassMarksEntity toEntity(ExamSectionCategoryPassMarksDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "examSection", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "stateId", ignore = true)
    void updateEntity(ExamSectionCategoryPassMarksDTO dto, @MappingTarget ExamSectionCategoryPassMarksEntity entity);

    List<ExamSectionCategoryPassMarksDTO> toDTOs(List<ExamSectionCategoryPassMarksEntity> entities);
    List<ExamSectionCategoryPassMarksEntity> toEntities(List<ExamSectionCategoryPassMarksDTO> dtos);

    @Named("uuidToExamSection")
    default ExamSectionPassMarksEntity uuidToExamSection(UUID id) {
        if (id == null) return null;
        ExamSectionPassMarksEntity e = new ExamSectionPassMarksEntity();
        e.setId(id);
        return e;
    }

    @Named("uuidToCategory")
    default ReservationCategoriesEntity uuidToCategory(UUID id) {
        if (id == null) return null;
        ReservationCategoriesEntity e = new ReservationCategoriesEntity();
        e.setId(id);
        return e;
    }
}
