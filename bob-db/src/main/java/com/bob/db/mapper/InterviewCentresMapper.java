package com.bob.db.mapper;

import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.entity.InterviewCentresEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InterviewCentresMapper {

    InterviewCentresDTO toDto(InterviewCentresEntity entity);
    List<InterviewCentresDTO> toDtoList(List<InterviewCentresEntity> entities);
    InterviewCentresEntity toEntity(InterviewCentresDTO dto);
    List<InterviewCentresEntity> toEntityList(List<InterviewCentresDTO> dtos);
    void updateEntityFromDto(InterviewCentresDTO dto, @MappingTarget InterviewCentresEntity entity);

}