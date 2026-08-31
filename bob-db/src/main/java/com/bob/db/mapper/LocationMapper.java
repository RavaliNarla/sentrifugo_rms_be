//package com.bob.db.mapper;
//
//import com.bob.db.dto.LocationDTO;
//import com.bob.db.entity.LocationEntity;
//import org.mapstruct.Mapper;
//import org.mapstruct.MappingTarget;
//import org.mapstruct.NullValuePropertyMappingStrategy;
//import org.mapstruct.ReportingPolicy;
//
//import java.util.List;
//
//@Mapper(
//        componentModel = "spring",
//        unmappedTargetPolicy = ReportingPolicy.IGNORE,
//        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//public interface LocationMapper {
//
//    LocationDTO toDto(LocationEntity locationEntity);
//
//    List<LocationDTO> toDtoList(List<LocationEntity> locationEntities);
//
//    LocationEntity toEntity(LocationDTO locationDTO);
//
//    List<LocationEntity> toEntityList(List<LocationDTO> locationDTOS);
//    void updateEntityFromDto(LocationDTO locationDTO,@MappingTarget LocationEntity locationEntity);
//}
//
