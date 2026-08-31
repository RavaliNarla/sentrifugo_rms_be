package com.bob.db.mapper;

import com.bob.db.dto.ConversationThreadsDTO;
import com.bob.db.entity.ConversationThreadsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ConversationThreadsMapper {

    ConversationThreadsEntity toEntity(ConversationThreadsDTO conversationThreadsDTO);

    ConversationThreadsDTO toDTO(ConversationThreadsEntity conversationThreadsEntity);

    List<ConversationThreadsDTO> toDTOList(List<ConversationThreadsEntity> conversationThreadsEntityList);

    List<ConversationThreadsEntity> toEntityList(List<ConversationThreadsDTO> conversationThreadsDTOList);

    void updateEntityFromDto(ConversationThreadsDTO conversationThreadsDTO,
                             @MappingTarget ConversationThreadsEntity conversationThreadsEntity);
}

