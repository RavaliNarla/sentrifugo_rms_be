package com.bob.db.mapper;

import com.bob.db.dto.LanguagesKnownDTO;
import com.bob.db.entity.LanguagesKnownEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LanguagesKnownMapper {
    LanguagesKnownDTO toDTO(LanguagesKnownEntity languagesKnownEntity);
    LanguagesKnownEntity toEntity(LanguagesKnownDTO languagesKnownDTO);
    List<LanguagesKnownDTO> toDTOList(List<LanguagesKnownEntity> languagesKnownEntities);
    List<LanguagesKnownEntity> toEntityList(List<LanguagesKnownDTO> languagesKnownDTOs);



    void updateEntityFromDto(
            LanguagesKnownDTO languagesKnownDTO,
            @MappingTarget LanguagesKnownEntity languagesKnownEntity
    );

}
