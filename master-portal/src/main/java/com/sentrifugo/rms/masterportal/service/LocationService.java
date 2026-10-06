package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.util.MasterCodeUtil;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.StateEntity;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.StateRepository;
import com.sentrifugo.rms.masterportal.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final StateRepository stateRepository;

    public List<LocationDTO> getAll(String search) {
        Map<UUID, String> stateNames = stateRepository.findAll().stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getName));
        List<LocationEntity> entities = (search == null || search.isBlank())
                ? locationRepository.findAllByOrderByNameAsc()
                : locationRepository.search(search.trim());
        return entities.stream()
                .map(e -> toDto(e, stateNames.get(e.getStateId())))
                .collect(Collectors.toList());
    }

    public Page<LocationDTO> search(String search, int page, int size) {
        Map<UUID, String> stateNames = stateRepository.findAll().stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getName));
        Pageable pageable = PageRequest.of(page, size);
        Page<LocationEntity> entities = (search == null || search.isBlank())
                ? locationRepository.findAllByOrderByNameAsc(pageable)
                : locationRepository.search(search.trim(), pageable);
        return entities.map(e -> toDto(e, stateNames.get(e.getStateId())));
    }

    public LocationDTO add(LocationDTO dto) {
        String code = MasterCodeUtil.normalizeRequired(dto.getCode(), "Location");
        if (locationRepository.existsByCodeIgnoreCase(code)) {
            throw new CommonException("A location with code '" + code + "' already exists.");
        }
        LocationEntity entity = LocationEntity.builder()
                .name(dto.getName().trim())
                .code(code)
                .stateId(dto.getStateId())
                .address(dto.getAddress())
                .build();
        locationRepository.save(entity);
        return toDto(entity, resolveStateName(dto.getStateId()));
    }

    public LocationDTO update(UUID id, LocationDTO dto) {
        LocationEntity entity = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        String code = MasterCodeUtil.normalizeRequired(dto.getCode(), "Location");
        if (locationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new CommonException("A location with code '" + code + "' already exists.");
        }
        entity.setName(dto.getName().trim());
        entity.setCode(code);
        entity.setStateId(dto.getStateId());
        entity.setAddress(dto.getAddress());
        locationRepository.save(entity);
        return toDto(entity, resolveStateName(dto.getStateId()));
    }

    public void delete(UUID id) {
        if (!locationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Location not found");
        }
        locationRepository.deleteById(id);
    }

    private String resolveStateName(UUID stateId) {
        return stateRepository.findById(stateId).map(StateEntity::getName).orElse(null);
    }

    private LocationDTO toDto(LocationEntity entity, String stateName) {
        return LocationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .stateId(entity.getStateId())
                .stateName(stateName)
                .address(entity.getAddress())
                .build();
    }
}
