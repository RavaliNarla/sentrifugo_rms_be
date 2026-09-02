package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.InterviewPanelEntity;
import com.sentrifugo.rms.db.entity.InterviewPanelMemberEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.InterviewPanelMemberRepository;
import com.sentrifugo.rms.db.repository.InterviewPanelRepository;
import com.sentrifugo.rms.db.repository.PositionPanelRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.recruiterportal.dto.InterviewPanelDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewPanelService {

    private final InterviewPanelRepository interviewPanelRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final UserRepository userRepository;
    private final PositionPanelRepository positionPanelRepository;

    @Transactional
    public InterviewPanelDTO create(InterviewPanelDTO dto) {
        if (interviewPanelRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new CommonException("A panel with this name already exists.");
        }
        InterviewPanelEntity panel = interviewPanelRepository.save(InterviewPanelEntity.builder().name(dto.getName()).build());
        saveMembers(panel.getId(), dto.getMemberIds());
        return toDto(panel);
    }

    @Transactional
    public InterviewPanelDTO update(UUID id, InterviewPanelDTO dto) {
        InterviewPanelEntity panel = interviewPanelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panel not found"));
        panel.setName(dto.getName());
        interviewPanelRepository.save(panel);
        interviewPanelMemberRepository.deleteByPanelId(id);
        saveMembers(id, dto.getMemberIds());
        return toDto(panel);
    }

    @Transactional
    public void delete(UUID id) {
        if (positionPanelRepository.existsByPanelId(id)) {
            throw new CommonException("Cannot delete a panel that is assigned to a position.");
        }
        interviewPanelMemberRepository.deleteByPanelId(id);
        interviewPanelRepository.deleteById(id);
    }

    public List<InterviewPanelDTO> getAll() {
        List<InterviewPanelEntity> panels = interviewPanelRepository.findAllByOrderByNameAsc();
        return panels.stream().map(this::toDto).collect(Collectors.toList());
    }

    private void saveMembers(UUID panelId, List<UUID> memberIds) {
        List<InterviewPanelMemberEntity> members = memberIds.stream()
                .map(userId -> InterviewPanelMemberEntity.builder().panelId(panelId).userId(userId).build())
                .collect(Collectors.toList());
        interviewPanelMemberRepository.saveAll(members);
    }

    private InterviewPanelDTO toDto(InterviewPanelEntity panel) {
        List<InterviewPanelMemberEntity> members = interviewPanelMemberRepository.findByPanelId(panel.getId());
        Map<UUID, String> userNames = userRepository.findAllById(
                members.stream().map(InterviewPanelMemberEntity::getUserId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getName));

        return InterviewPanelDTO.builder()
                .id(panel.getId())
                .name(panel.getName())
                .memberIds(members.stream().map(InterviewPanelMemberEntity::getUserId).collect(Collectors.toList()))
                .memberNames(members.stream().map(m -> userNames.get(m.getUserId())).collect(Collectors.toList()))
                .build();
    }
}
