package com.bob.db.mapper;

import com.bob.db.dto.DistrictDTO;
import com.bob.db.entity.DistrictEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DistrictMapper {
    DistrictDTO toDTO(DistrictEntity entity);

    DistrictEntity toEntity(DistrictDTO dto);

    List<DistrictDTO> toDTOList(List<DistrictEntity> entities);

    List<DistrictEntity> toEntityList(List<DistrictDTO> dtos);

    void updateEntityFromDto(DistrictDTO dto,
                             @MappingTarget DistrictEntity entity);
}
