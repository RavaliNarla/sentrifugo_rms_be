//package com.bob.masterdata.Service;
//
//import com.bob.commonutil.service.ExcelTemplateService;
//import com.bob.commonutil.util.AppConstants;
//import com.bob.commonutil.exception.ExcelValidationException;
//import com.bob.masterdata.validators.LocationValidator;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.transaction.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//public class LocationService {
//    @Autowired
//    private LocationRepository locationRepository;
//
//    @Autowired
//    private LocationMapper locationMapper;
//
//    @Autowired
//    private ExcelTemplateService excelTemplateService;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Autowired
//    private LocationValidator locationValidator;
//
//    public LocationDTO createLocation(LocationDTO location) {
//        try {
//            LocationEntity locationEntity=locationMapper.toEntity(location);
//            locationRepository.save(locationEntity);
//            return location;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    public List<LocationDTO> getAllLocations() throws Exception {
//        try {
//            return locationMapper.toDtoList(locationRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE)));
//        } catch (Exception e) {
//            throw new Exception("Failed to fetch locations");
//        }
//    }
//
//    public LocationDTO updateLocations(UUID id, LocationDTO location) {
//        LocationEntity locationEntity=locationRepository.findById(id).orElseThrow(()->new RuntimeException("Location not found"));
//        locationMapper.updateEntityFromDto(location,locationEntity);
//        locationRepository.save(locationEntity);
//        return locationMapper.toDto(locationEntity);
//    }
//
//    public LocationDTO deleteLocation(UUID id) {
//        try {
//            if (locationRepository.existsById(id)) {
//                LocationEntity location=locationRepository.findById(id).get();
////                location.setIsActive(false);
////                locationRepository.save(location);
//                locationRepository.deleteById(id);
//                return locationMapper.toDto(location);
//            } else {
//                throw new Exception("ID DOESN'T EXIST");
//            }
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Transactional
//    public List<LocationDTO> bulkSave(MultipartFile file) {
//        try {
//            List<LocationDTO> locationDTOS =
//                    excelTemplateService.excelToDto(
//                            file.getInputStream(),
//                            LocationDTO.class
//                    );
//
//            List<String> errors = new ArrayList<>();
//
//            errors.addAll(
//                    locationValidator.collectValidationErrors(locationDTOS)
//            );
//
//            if (!errors.isEmpty()) {
//                throw new ExcelValidationException(errors);
//            }
//
//            List<LocationEntity> locationEntities =
//                    locationMapper.toEntityList(locationDTOS);
//
//            return locationMapper.toDtoList(
//                    locationRepository.saveAll(locationEntities)
//            );
//
//        } catch (IOException e) {
//            throw new RuntimeException(
//                    "Fail to store excel data: " + e.getMessage(),
//                    e
//            );
//        }
//    }
//
//
//
//}
