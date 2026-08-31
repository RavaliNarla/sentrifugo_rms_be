package com.bob.db.mapper;

import com.bob.db.dto.MasterPositionsDTO;
import com.bob.db.entity.MasterPositionsEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MasterPositionsMapper {
    MasterPositionsMapper INSTANCE = Mappers.getMapper(MasterPositionsMapper.class);

    MasterPositionsDTO toDTO(MasterPositionsEntity entity);

    MasterPositionsEntity toEntity(MasterPositionsDTO dto);

    List<MasterPositionsDTO> toDTOList(List<MasterPositionsEntity> entities);

    List<MasterPositionsEntity> toEntityList(List<MasterPositionsDTO> dtos);

    void updateEntityFromDto(MasterPositionsDTO dto,@MappingTarget MasterPositionsEntity entity);
}
