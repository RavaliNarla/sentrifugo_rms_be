package com.bob.db.mapper;

import com.bob.db.dto.ExamCenterDTO;
import com.bob.db.entity.ExamCenterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExamCenterMapper {
    ExamCenterEntity toEntity(ExamCenterDTO examCenterDTO);
    ExamCenterDTO toDto(ExamCenterEntity examCenterEntity);
    void updateEntityFromDto(ExamCenterDTO dto, @MappingTarget ExamCenterEntity entity);
    List<ExamCenterEntity> toEntityList(List<ExamCenterDTO> examCenterDTOList);
    List<ExamCenterDTO> toDtoList(List<ExamCenterEntity> examCenterEntityList);
}
