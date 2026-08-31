package com.bob.db.mapper;

import com.bob.db.dto.JobPositionEmbeddingsDTO;
import com.bob.db.entity.JobPositionEmbeddingsEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface JobPositionEmbeddingsMapper {
    JobPositionEmbeddingsMapper INSTANCE = Mappers.getMapper(JobPositionEmbeddingsMapper.class);

    JobPositionEmbeddingsDTO toDTO(JobPositionEmbeddingsEntity entity);

    JobPositionEmbeddingsEntity toEntity(JobPositionEmbeddingsDTO dto);

    List<JobPositionEmbeddingsDTO> toDTOList(List<JobPositionEmbeddingsEntity> entities);

    List<JobPositionEmbeddingsEntity> toEntityList(List<JobPositionEmbeddingsDTO> dtos);

    void updateEntityFromDto(JobPositionEmbeddingsDTO dto, @MappingTarget JobPositionEmbeddingsEntity entity);
}

