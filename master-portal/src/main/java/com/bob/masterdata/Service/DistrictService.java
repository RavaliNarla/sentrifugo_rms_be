package com.bob.masterdata.Service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.DistrictDTO;
import com.bob.db.entity.DistrictEntity;
import com.bob.db.mapper.DistrictMapper;
import com.bob.db.repository.DistrictRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DistrictService {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private DistrictMapper districtMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public DistrictDTO addDistrict(DistrictDTO districtDTO) {
        DistrictEntity entity = districtMapper.toEntity(districtDTO);
        DistrictEntity savedEntity = districtRepository.save(entity);
        return districtMapper.toDTO(savedEntity);
    }

    public List<DistrictDTO> getAllDistricts() {
        List<DistrictEntity> entities = districtRepository.findAll();
        return districtMapper.toDTOList(entities);
    }

    public DistrictDTO updateDistrict(UUID id, DistrictDTO districtDTO) {
        DistrictEntity entity = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found"));
        districtMapper.updateEntityFromDto(districtDTO, entity);
        return districtMapper.toDTO(districtRepository.save(entity));
    }

    public DistrictDTO deleteDistrict(UUID id) {
        DistrictEntity entity = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found"));
        districtRepository.deleteById(id); // soft delete via @SQLDelete
        return districtMapper.toDTO(entity);
    }

//    public List<DistrictDTO> bulkSave(MultipartFile file) {
//        try {
//            List<DistrictDTO> districtDTOS = excelTemplateService.excelToDto(file.getInputStream(), DistrictDTO.class);
//            BulkValidationUtils.validateNoDuplicates(
//                    districtDTOS,
//                    DistrictDTO::getDistrictName,
//                    entityManager,
//                    DistrictEntity.class,
//                    "districtName",
//                    "District"
//            );
//            List<DistrictEntity> cityEntities = districtMapper.toEntityList(districtDTOS);
//            return districtMapper.toDTOList(districtRepository.saveAll(cityEntities));
//        } catch (IOException e) {
//            throw new RuntimeException("fail to store excel data: " + e.getMessage());
//        }
//
//    }
}
