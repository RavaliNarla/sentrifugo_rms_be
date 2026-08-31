package com.bob.db.mapper;

import com.bob.db.dto.PanelMembersScoreDTO;
import com.bob.db.entity.PanelMembersScoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PanelMembersScoreMapper {
    PanelMembersScoreDTO toDto(PanelMembersScoreEntity entity);
    PanelMembersScoreEntity toEntity(PanelMembersScoreDTO dto);
    List<PanelMembersScoreDTO> toDtoList(List<PanelMembersScoreEntity> entities);
    List<PanelMembersScoreEntity> toEntityList(List<PanelMembersScoreDTO> dtos);
    void updateEntityFromDto(PanelMembersScoreDTO dto,@MappingTarget PanelMembersScoreEntity entity);
}
