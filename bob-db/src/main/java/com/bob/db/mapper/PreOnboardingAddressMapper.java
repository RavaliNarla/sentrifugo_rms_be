package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingAddressDTO;
import com.bob.db.entity.PreOnboardingAddressEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {PreOnboardingDocumentMapper.class} // Reuses your existing document mapper rules
)
public abstract class PreOnboardingAddressMapper {

    public abstract PreOnboardingAddressDTO toDTO(PreOnboardingAddressEntity entity);

    public abstract List<PreOnboardingAddressDTO> toDTOList(List<PreOnboardingAddressEntity> entityList);


    public abstract PreOnboardingAddressEntity toEntity(PreOnboardingAddressDTO dto);

    public abstract List<PreOnboardingAddressEntity> toEntityList(List<PreOnboardingAddressDTO> dtoList);

    public abstract void updateEntityFromDto(PreOnboardingAddressDTO dto, @MappingTarget PreOnboardingAddressEntity entity);
}