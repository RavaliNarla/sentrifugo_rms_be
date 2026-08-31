package com.bob.db.mapper;


import com.bob.db.dto.ConversationMessagesDTO;
import com.bob.db.entity.ConversationMessagesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ConversationMessagesMapper {

    ConversationMessagesEntity toEntity(ConversationMessagesDTO conversationMessagesDTO);
    ConversationMessagesDTO toDto(ConversationMessagesEntity conversationMessagesEntity);
    List<ConversationMessagesDTO> toDtoList(List<ConversationMessagesEntity> conversationMessagesEntities);
    List<ConversationMessagesEntity> toEntityList(List<ConversationMessagesDTO> conversationMessagesDTOS);
    void updateEntityFromDto(ConversationMessagesDTO conversationMessagesDTO,@MappingTarget ConversationMessagesEntity conversationMessagesEntity);
}

