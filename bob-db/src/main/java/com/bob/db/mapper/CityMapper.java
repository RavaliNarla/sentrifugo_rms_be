package com.bob.db.mapper;

import com.bob.db.dto.CityDTO;
import com.bob.db.entity.CityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CityMapper {

    CityDTO toDto(CityEntity cityEntity);
    List<CityDTO> toDtoList(List<CityEntity> cityEntities);
    CityEntity toEntity(CityDTO cityDto);
    List<CityEntity> toEntityList(List<CityDTO> cityDTOS);
    void updateEntityFromDto(CityDTO cityDto,@MappingTarget CityEntity cityEntity);
}