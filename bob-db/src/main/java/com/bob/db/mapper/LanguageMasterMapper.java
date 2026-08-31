package com.bob.db.mapper;

import com.bob.db.dto.LanguageMasterDTO;
import com.bob.db.entity.LanguageMasterEntity;
import com.bob.db.entity.LanguagesKnownEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LanguageMasterMapper {
    LanguageMasterDTO toDto(LanguageMasterEntity entity);
    LanguageMasterEntity toEntity(LanguageMasterDTO dto);

    List<LanguagesKnownEntity> toEntityList(List<LanguageMasterDTO> dtos);
    List<LanguageMasterDTO> toDtoList(List<LanguageMasterEntity> entities);

}
