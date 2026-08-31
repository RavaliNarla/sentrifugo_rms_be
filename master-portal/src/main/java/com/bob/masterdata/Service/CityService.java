package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.CityDTO;
import com.bob.db.entity.CityEntity;
import com.bob.db.mapper.CityMapper;
import com.bob.db.repository.CityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CityService {
    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public CityDTO createCity(CityDTO city) {
        try {
            cityRepository.save(cityMapper.toEntity(city));
            return city;
        } catch (Exception e) {
            return null;
        }
    }

    public List<CityDTO> getAllCities(){
        try {
            return cityMapper.toDtoList(cityRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch cities");
        }
    }

    public CityDTO updateCities(UUID id, CityDTO city) {
        CityEntity cityEntity=cityRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("City not found"));
        cityMapper.updateEntityFromDto(city,cityEntity);
        return cityMapper.toDto(cityRepository.save(cityEntity));
    }

    public CityDTO deleteCities(UUID id) {

                CityEntity cityEntity=cityRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("City not found"));
                cityRepository.delete(cityEntity);
                CityDTO city=cityMapper.toDto(cityEntity);
                return city;

    }

}
