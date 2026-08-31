package com.bob.db.mapper;

import com.bob.db.dto.InterviewPanelMembersDTO;
import com.bob.db.dto.UserDTO;
import com.bob.db.entity.InterviewPanelMembersEntity;
import com.bob.db.entity.UserEntity;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {UserMapper.class}
)
public abstract class InterviewPanelMembersMapper {

    @Autowired
    protected UserMapper userMapper;

    @Mapping(target = "panel", ignore = true)
    @Mapping(target = "panelMember", source = "panelMember", qualifiedByName = "userDtoToUserEntity")
    public abstract InterviewPanelMembersEntity toEntity(InterviewPanelMembersDTO interviewPanelMembersDTO);

    @Mapping(target = "panelId", source = "panel.id")
    @Mapping(target = "panelMember", source = "panelMember")
    public abstract InterviewPanelMembersDTO toDto(InterviewPanelMembersEntity interviewPanelMembersEntity);

    @Mapping(target = "panelId", source = "panel.id")
    @Mapping(target = "panelMember", source = "panelMember")
    public abstract InterviewPanelMembersDTO toDto(InterviewPanelMembersEntity interviewPanelMembersEntity, @Context Map<UUID, UserEntity> userMap);

    public List<InterviewPanelMembersDTO> toDtoList(List<InterviewPanelMembersEntity> interviewPanelMembersEntities, @Context Map<UUID, UserEntity> userMap) {
        return interviewPanelMembersEntities.stream()
                .map(entity -> toDto(entity, userMap))
                .collect(java.util.stream.Collectors.toList());
    }

    public abstract List<InterviewPanelMembersEntity> toEntityList(List<InterviewPanelMembersDTO> interviewPanelMembersDTOS);

    @Mapping(target = "panel", ignore = true)
    @Mapping(target = "panelMember", source = "panelMember", qualifiedByName = "userDtoToUserEntity")
    public abstract void updateEntityFromDto(InterviewPanelMembersDTO interviewPanelMembersDTO, @MappingTarget InterviewPanelMembersEntity interviewPanelMembersEntity);

    @Named("userDtoToUserEntity")
    protected UserEntity userDtoToUserEntity(UserDTO userDTO) {
        if (userDTO == null || userDTO.getId() == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userDTO.getId());
        return userEntity;
    }

}
