package com.bob.db.mapper;

import com.bob.db.dto.CaptchaDTO;
import com.bob.db.entity.CaptchaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CaptchaMapper {

    CaptchaDTO toDto(CaptchaEntity entity);

    CaptchaEntity toEntity(CaptchaDTO dto);

    void updateEntityFromDto(CaptchaDTO dto, @MappingTarget CaptchaEntity entity);
}
