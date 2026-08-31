package com.bob.db.mapper;

import com.bob.db.dto.CcAvenueOrdersDTO;
import com.bob.db.entity.CcAvenueOrdersEntity;
import com.bob.db.enums.CcAvenueOrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CcAvenueOrdersMapper {
    @Mapping(source = "status", target = "status")
    CcAvenueOrdersEntity toEntity(CcAvenueOrdersDTO ccAvenueOrdersDTO);
    CcAvenueOrdersDTO toDTO(CcAvenueOrdersEntity ccAvenueOrdersEntity);
    List<CcAvenueOrdersDTO> toDTOList(List<CcAvenueOrdersEntity> ccAvenueOrdersEntities);
    List<CcAvenueOrdersEntity> toEntityList(List<CcAvenueOrdersDTO> ccAvenueOrdersDTOS);
    @Mapping(source = "dto.status", target = "entity.status")
    void updateEntityFromDto(CcAvenueOrdersDTO dto, @MappingTarget CcAvenueOrdersEntity entity);

    default CcAvenueOrderStatus mapStringToCcAvenueOrderStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return CcAvenueOrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log the error or handle it as per your application's policy
            // For now, returning UNKNOWN or throwing a custom exception might be appropriate
            return CcAvenueOrderStatus.UNKNOWN; // Or throw new InvalidArgumentException("Invalid status: " + status);
        }
    }
}
