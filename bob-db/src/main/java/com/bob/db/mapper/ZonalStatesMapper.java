package com.bob.db.mapper;

import com.bob.db.dto.ZonalStatesDTO;
import com.bob.db.entity.ZonalStatesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ZonalStatesMapper {

    ZonalStatesDTO toDto(ZonalStatesEntity zonalStatesEntity);

    List<ZonalStatesDTO> toDtoList(List<ZonalStatesEntity> zonalStatesEntities);

    ZonalStatesEntity toEntity(ZonalStatesDTO zonalStatesDTO);

    List<ZonalStatesEntity> toEntityList(List<ZonalStatesDTO> zonalStatesDTOs);

    void updateEntityFromDto(ZonalStatesDTO zonalStatesDTO, @MappingTarget ZonalStatesEntity zonalStatesEntity);

}
