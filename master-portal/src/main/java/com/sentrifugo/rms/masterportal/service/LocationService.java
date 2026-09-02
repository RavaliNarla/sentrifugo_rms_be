package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.StateEntity;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.StateRepository;
import com.sentrifugo.rms.masterportal.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
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

    public List<LocationDTO> getAll() {
        Map<UUID, String> stateNames = stateRepository.findAll().stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getName));
        return locationRepository.findAllByOrderByNameAsc().stream()
                .map(e -> toDto(e, stateNames.get(e.getStateId())))
                .collect(Collectors.toList());
    }

    public LocationDTO add(LocationDTO dto) {
        LocationEntity entity = LocationEntity.builder()
                .name(dto.getName())
                .stateId(dto.getStateId())
                .address(dto.getAddress())
                .build();
        locationRepository.save(entity);
        return toDto(entity, resolveStateName(dto.getStateId()));
    }

    public LocationDTO update(UUID id, LocationDTO dto) {
        LocationEntity entity = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        entity.setName(dto.getName());
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
                .stateId(entity.getStateId())
                .stateName(stateName)
                .address(entity.getAddress())
                .build();
    }
}
