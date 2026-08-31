package com.bob.db.mapper;

import com.bob.db.dto.PositionCategoryDistributionDTO;
import com.bob.db.dto.PositionStateDistributionDTO;
import com.bob.db.entity.PositionCategoryDistributionEntity;
import com.bob.db.entity.PositionStateDistributionEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PositionCategoryDistributionMapper {

    @Mapping(target = "positionStateDistribution.id", source = "stateDistributionId")
    PositionCategoryDistributionEntity toEntity(PositionCategoryDistributionDTO positionCategoryDistributionDTO);

    @Mapping(target = "stateDistributionId", source = "positionStateDistribution.id")
    PositionCategoryDistributionDTO toDTO(PositionCategoryDistributionEntity positionCategoryDistributionEntity);

    List<PositionCategoryDistributionDTO> toDTOList(List<PositionCategoryDistributionEntity> positionCategoryDistributionEntityList);

    List<PositionCategoryDistributionEntity> toEntityList(List<PositionCategoryDistributionDTO> positionCategoryDistributionDTOList);

    @InheritConfiguration(name = "toEntity")
    void updateEntityFromDto(PositionCategoryDistributionDTO positionCategoryDistributionDTO,
                             @MappingTarget PositionCategoryDistributionEntity positionCategoryDistributionEntity);
}
