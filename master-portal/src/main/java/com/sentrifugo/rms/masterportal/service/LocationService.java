package com.sentrifugo.rms.masterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.StateEntity;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.StateRepository;
import com.sentrifugo.rms.masterportal.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final StateRepository stateRepository;
    private final MasterSyncService masterSyncService;

    public List<LocationDTO> getAll(String search, String hrmsToken) {
        safeSync(hrmsToken);
        Map<UUID, String> stateNames = stateRepository.findAll().stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getName));
        List<LocationEntity> entities = (search == null || search.isBlank())
                ? locationRepository.findAllByOrderByNameAsc()
                : locationRepository.search(search.trim());
        return entities.stream()
                .map(e -> toDto(e, e.getStateId() == null ? null : stateNames.get(e.getStateId())))
                .collect(Collectors.toList());
    }

    public Page<LocationDTO> search(String search, int page, int size, String hrmsToken) {
        safeSync(hrmsToken);
        Map<UUID, String> stateNames = stateRepository.findAll().stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getName));
        Pageable pageable = PageRequest.of(page, size);
        Page<LocationEntity> entities = (search == null || search.isBlank())
                ? locationRepository.findAllByOrderByNameAsc(pageable)
                : locationRepository.search(search.trim(), pageable);
        return entities.map(e -> toDto(e, e.getStateId() == null ? null : stateNames.get(e.getStateId())));
    }

    public LocationDTO add(LocationDTO dto) {
        throw new CommonException("Locations are synced from HRMS business units. Add them in HRMS, then refresh this list.");
    }

    public LocationDTO update(UUID id, LocationDTO dto) {
        throw new CommonException("Locations are synced from HRMS business units and cannot be edited in RMS.");
    }

    public void delete(UUID id) {
        throw new CommonException("Locations cannot be deleted in RMS. Deactivate the business unit in HRMS.");
    }

    private void safeSync(String hrmsToken) {
        try {
            masterSyncService.syncLocations(hrmsToken);
        } catch (Exception ex) {
            log.warn("Location sync failed; returning existing RMS copy: {}", ex.getMessage());
        }
    }

    private LocationDTO toDto(LocationEntity entity, String stateName) {
        return LocationDTO.builder()
                .id(entity.getId())
                .hrmsId(entity.getHrmsId())
                .name(entity.getName())
                .code(entity.getCode())
                .stateId(entity.getStateId())
                .stateName(stateName)
                .address(entity.getAddress())
                .build();
    }
}
