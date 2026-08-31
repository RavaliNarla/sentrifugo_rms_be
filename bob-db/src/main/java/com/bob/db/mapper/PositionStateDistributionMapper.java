package com.bob.db.mapper;

import com.bob.db.dto.PositionStateDistributionDTO;
import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.entity.PositionStateDistributionEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {PositionCategoryDistributionMapper.class})
public interface PositionStateDistributionMapper {

    @Mapping(target = "jobPosition.id", source = "positionId")
    PositionStateDistributionEntity toEntity(PositionStateDistributionDTO positionStateDistributionDTO);

    @Mapping(target = "positionId", source = "jobPosition.id")
    PositionStateDistributionDTO toDTO(PositionStateDistributionEntity positionStateDistributionEntity);

    List<PositionStateDistributionDTO> toDTOList(List<PositionStateDistributionEntity> positionStateDistributionEntityList);

    List<PositionStateDistributionEntity> toEntityList(List<PositionStateDistributionDTO> positionStateDistributionDTOList);

    @InheritConfiguration(name = "toEntity")
    void updateEntityFromDto(PositionStateDistributionDTO positionStateDistributionDTO,
                             @MappingTarget PositionStateDistributionEntity positionStateDistributionEntity);

    @AfterMapping
    default void linkChildren(PositionStateDistributionDTO dto, @MappingTarget PositionStateDistributionEntity entity) {
        if (entity.getPositionCategoryDistributions() != null) {
            entity.getPositionCategoryDistributions().forEach(child -> {
                child.setPositionStateDistribution(entity);
            });
        }
    }
}
