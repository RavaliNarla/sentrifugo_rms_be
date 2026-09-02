package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.InterviewPanelEntity;
import com.sentrifugo.rms.db.entity.PositionPanelEntity;
import com.sentrifugo.rms.db.repository.InterviewPanelRepository;
import com.sentrifugo.rms.db.repository.PositionPanelRepository;
import com.sentrifugo.rms.recruiterportal.dto.PositionPanelDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionPanelService {

    private final PositionPanelRepository positionPanelRepository;
    private final InterviewPanelRepository interviewPanelRepository;

    public PositionPanelDTO assign(PositionPanelDTO dto) {
        PositionPanelEntity entity = PositionPanelEntity.builder()
                .positionId(dto.getPositionId())
                .panelId(dto.getPanelId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        return toDto(positionPanelRepository.save(entity));
    }

    public void remove(UUID id) {
        if (!positionPanelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Position panel assignment not found");
        }
        positionPanelRepository.deleteById(id);
    }

    public List<PositionPanelDTO> getByPositionId(UUID positionId) {
        return positionPanelRepository.findByPositionId(positionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Panels assigned to a position whose date range hasn't ended yet - usable for scheduling. */
    public List<PositionPanelDTO> getActiveByPositionId(UUID positionId) {
        return positionPanelRepository.findByPositionIdAndEndDateGreaterThanEqual(positionId, LocalDate.now()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PositionPanelDTO toDto(PositionPanelEntity entity) {
        Map<UUID, String> panelNames = interviewPanelRepository.findAll().stream()
                .collect(Collectors.toMap(InterviewPanelEntity::getId, InterviewPanelEntity::getName));
        return PositionPanelDTO.builder()
                .id(entity.getId())
                .positionId(entity.getPositionId())
                .panelId(entity.getPanelId())
                .panelName(panelNames.get(entity.getPanelId()))
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }
}
