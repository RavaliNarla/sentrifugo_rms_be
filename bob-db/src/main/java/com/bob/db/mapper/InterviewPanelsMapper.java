package com.bob.db.mapper;

import com.bob.db.dto.InterviewCommitteeDTO;
import com.bob.db.dto.InterviewPanelsDTO;
import com.bob.db.entity.InterviewCommitteeEntity;
import com.bob.db.entity.InterviewPanelsEntity;
import com.bob.db.entity.UserEntity;
import org.mapstruct.*;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {InterviewPanelMembersMapper.class, InterviewCommitteeMapper.class}
)
public abstract class InterviewPanelsMapper {

    // Removed @Autowired protected InterviewCommitteeMapper interviewCommitteeMapper;

    @Mapping(target = "committee", source = "committee")
    @Mapping(target = "panelMembers", source = "panelMembers")
    public abstract InterviewPanelsEntity toEntity(InterviewPanelsDTO interviewPanelsDTO);

    // Reverted to simple source mapping for committee
    @Mapping(target = "committee", source = "committee")
    @Mapping(target = "panelMembers", source = "panelMembers")
    public abstract InterviewPanelsDTO toDto(
            InterviewPanelsEntity interviewPanelsEntity,
            @Context Map<UUID, UserEntity> userMap,
            @Context Map<UUID, InterviewCommitteeEntity> committeeMap);

    public List<InterviewPanelsDTO> toDtoList(List<InterviewPanelsEntity> interviewPanelsEntities,
                                              @Context Map<UUID, UserEntity> userMap,
                                              @Context Map<UUID, InterviewCommitteeEntity> committeeMap) {
        if (interviewPanelsEntities == null || interviewPanelsEntities.isEmpty()) {
            return Collections.emptyList();
        }
        return interviewPanelsEntities.stream()
                .map(entity -> toDto(entity, userMap, committeeMap))
                .collect(Collectors.toList());
    }

    public abstract List<InterviewPanelsEntity> toEntityList(List<InterviewPanelsDTO> interviewPanelsDTOS);

    @Mapping(target = "committee", source = "committee", qualifiedByName = "committeeDtoToCommitteeEntity")
    @Mapping(target = "panelMembers", ignore = true)
    public abstract void updateEntityFromDto(InterviewPanelsDTO interviewPanelsDTO, @MappingTarget InterviewPanelsEntity interviewPanelsEntity);

    @Named("committeeDtoToCommitteeEntity")
    protected InterviewCommitteeEntity committeeDtoToCommitteeEntity(InterviewCommitteeDTO committeeDTO) {
        if (committeeDTO == null || committeeDTO.getId() == null) {
            return null;
        }
        InterviewCommitteeEntity committeeEntity = new InterviewCommitteeEntity();
        committeeEntity.setId(committeeDTO.getId());
        return committeeEntity;
    }

    @Named("committeeEntityToDtoWithoutChildren")
    protected InterviewCommitteeDTO committeeEntityToDtoWithoutChildren(InterviewCommitteeEntity committeeEntity) {
        if (committeeEntity == null || committeeEntity.getId() == null) {
            return null;
        }
        InterviewCommitteeDTO committeeDTO = new InterviewCommitteeDTO();
        committeeDTO.setId(committeeEntity.getId());
        return committeeDTO;
    }

    @Named("toEntityWithoutChildren")
    @Mapping(target = "committee", source = "committee", qualifiedByName = "committeeDtoToCommitteeEntity")
    @Mapping(target = "panelMembers", ignore = true)
    public abstract InterviewPanelsEntity toEntityWithoutChildren(InterviewPanelsDTO interviewPanelsDTO);

    @Named("toDtoWithoutChildren")
    @Mapping(target = "committee", qualifiedByName = "committeeEntityToDtoWithoutChildren")
    @Mapping(target = "panelMembers", ignore = true)
    public abstract InterviewPanelsDTO toDtoWithoutChildren(InterviewPanelsEntity interviewPanelsEntity);

    @Named("toEntityListWithoutChildren")
    public abstract List<InterviewPanelsEntity> toEntityListWithoutChildren(List<InterviewPanelsDTO> dtos);

    @Named("toDtoListWithoutChildren")
    public abstract List<InterviewPanelsDTO> toDtoListWithoutChildren(List<InterviewPanelsEntity> entities);
}
