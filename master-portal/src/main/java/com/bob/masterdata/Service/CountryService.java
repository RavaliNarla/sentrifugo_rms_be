package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.CountryDTO;
import com.bob.db.entity.CountryEntity;
import com.bob.db.mapper.CountryMapper;
import com.bob.db.repository.CountryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CountryService {

    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CountryMapper countryMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public CountryDTO createCountry(CountryDTO country) {
        try {
            CountryEntity countryEntity=countryMapper.toEntity(country);
            countryRepository.save(countryEntity);
            return countryMapper.toDto(countryEntity);
        } catch (Exception e) {
            return null;
        }
    }

    public List<CountryDTO> getAllCountries(){
        try {
            return countryMapper.toDtoList(countryRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch countries");
        }
    }

    public CountryDTO updateCountry(UUID id, CountryDTO country) {
        CountryEntity countryEntity=countryRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Country not found"));
        countryMapper.updateEntityFromDto(country,countryEntity);
        return countryMapper.toDto(countryRepository.save(countryEntity));
    }

    public CountryDTO deleteCountry(UUID id) {
        CountryEntity countryEntity=countryRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Country not found"));
        countryRepository.delete(countryEntity);
        CountryDTO country=countryMapper.toDto(countryEntity);
        return country;
    }


}
