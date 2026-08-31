package com.bob.db.mapper;

import com.bob.db.dto.PositionPanelDTO;
import com.bob.db.entity.PositionPanelEntity;
import com.bob.db.entity.UserEntity;
import com.bob.db.entity.InterviewCommitteeEntity;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {InterviewPanelsMapper.class}
)
public interface PositionPanelMapper {

    @Mapping(target = "positionId", source = "jobPosition.id")
    PositionPanelDTO toDtoWithoutChildren(PositionPanelEntity entity);

    @Mapping(target = "interviewPanel", source = "interviewPanel")
    @Mapping(target = "positionId", source = "jobPosition.id")
    PositionPanelDTO toDto(PositionPanelEntity entity, @Context Map<UUID, UserEntity> userMap, @Context Map<UUID, InterviewCommitteeEntity> committeeMap);

    @Mapping(target = "interviewPanel", source = "interviewPanel")
    PositionPanelEntity toEntity(PositionPanelDTO dto);

    @Named("toDtoListWithoutChildren")
    List<PositionPanelDTO> toDtoListWithoutChildren(List<PositionPanelEntity> positionPanelEntityList);

    @Named("toEntityListWithoutChildren")
    List<PositionPanelEntity> toEntityListWithoutChildren(List<PositionPanelDTO> positionPanelDTOS);

    @Mapping(target = "id", ignore = true) // ID should not be updated
    @Mapping(target = "jobPosition", ignore = true) // JobPosition should not be updated via DTO
    @Mapping(target = "interviewPanel", ignore = true) // InterviewPanel should not be updated via DTO
    void updateEntityFromDto(PositionPanelDTO dto, @MappingTarget PositionPanelEntity entity);
}
