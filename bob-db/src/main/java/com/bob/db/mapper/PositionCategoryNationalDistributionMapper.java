package com.bob.db.mapper;

import com.bob.db.dto.PositionCategoryNationalDistributionDTO;
import com.bob.db.entity.PositionCategoryNationalDistributionEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PositionCategoryNationalDistributionMapper {

    @Mapping(target = "jobPosition.id", source = "jobPositionId")
    PositionCategoryNationalDistributionEntity toEntity(PositionCategoryNationalDistributionDTO positionCategoryNationalDistributionDTO);

    @Mapping(target = "jobPositionId", source = "jobPosition.id")
    PositionCategoryNationalDistributionDTO toDTO(PositionCategoryNationalDistributionEntity positionCategoryNationalDistributionEntity);

    List<PositionCategoryNationalDistributionDTO> toDTOList(List<PositionCategoryNationalDistributionEntity> positionCategoryNationalDistributionEntityList);

    List<PositionCategoryNationalDistributionEntity> toEntityList(List<PositionCategoryNationalDistributionDTO> positionCategoryNationalDistributionDTOList);

    @InheritConfiguration(name = "toEntity")
    void updateEntityFromDto(PositionCategoryNationalDistributionDTO positionCategoryNationalDistributionDTO,
                             @MappingTarget PositionCategoryNationalDistributionEntity positionCategoryNationalDistributionEntity);
}
