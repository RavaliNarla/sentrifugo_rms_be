package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.InterviewPanelEntity;
import com.sentrifugo.rms.db.entity.InterviewPanelMemberEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.repository.InterviewPanelMemberRepository;
import com.sentrifugo.rms.db.repository.InterviewPanelRepository;
import com.sentrifugo.rms.db.repository.InterviewRoundMetaRepository;
import com.sentrifugo.rms.db.repository.InterviewScheduleRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.recruiterportal.dto.InterviewPanelDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewPanelService {

    private final InterviewPanelRepository interviewPanelRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final UserRepository userRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewRoundMetaRepository interviewRoundMetaRepository;

    @Transactional
    public InterviewPanelDTO create(InterviewPanelDTO dto) {
        if (interviewPanelRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new CommonException("A panel with this name already exists.");
        }
        assertHomogeneousMemberRoles(dto.getMemberIds());
        rejectIfDuplicateMemberSet(dto.getMemberIds(), null);
        InterviewPanelEntity panel = interviewPanelRepository.save(InterviewPanelEntity.builder().name(dto.getName()).build());
        saveMembers(panel.getId(), dto.getMemberIds());
        return toDto(panel);
    }

    @Transactional
    public InterviewPanelDTO update(UUID id, InterviewPanelDTO dto) {
        InterviewPanelEntity panel = interviewPanelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panel not found"));
        if (panelHasSchedules(id)) {
            throw new CommonException("Cannot edit a panel that already has interview schedules.");
        }
        assertHomogeneousMemberRoles(dto.getMemberIds());
        rejectIfDuplicateMemberSet(dto.getMemberIds(), id);
        panel.setName(dto.getName());
        interviewPanelRepository.save(panel);
        interviewPanelMemberRepository.deleteByPanelId(id);
        saveMembers(id, dto.getMemberIds());
        return toDto(panel);
    }

    /** Panels are recruiter-only or committee-only — never mixed (corporate scoring separation). */
    private void assertHomogeneousMemberRoles(List<UUID> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        List<UserEntity> users = userRepository.findAllById(memberIds);
        if (users.size() != memberIds.stream().distinct().count()) {
            throw new CommonException("One or more selected panel members were not found.");
        }
        Set<String> roles = users.stream()
                .map(UserEntity::getRole)
                .filter(Objects::nonNull)
                .map(r -> r.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (roles.size() > 1) {
            throw new CommonException(
                    "A panel must contain only Recruiters or only Committee Members — mixed roles are not allowed.");
        }
    }

    // SCL: block creating/renaming a panel to have the exact same member set as an existing
    // panel (order-independent) - the recruiter should reuse the existing panel instead.
    private void rejectIfDuplicateMemberSet(List<UUID> memberIds, UUID excludePanelId) {
        Set<UUID> incoming = new HashSet<>(memberIds);
        List<InterviewPanelEntity> allPanels = interviewPanelRepository.findAll();
        List<UUID> candidateIds = allPanels.stream()
                .map(InterviewPanelEntity::getId)
                .filter(pid -> excludePanelId == null || !pid.equals(excludePanelId))
                .collect(Collectors.toList());
        if (candidateIds.isEmpty()) {
            return;
        }
        Map<UUID, Set<UUID>> membersByPanel = interviewPanelMemberRepository.findByPanelIdIn(candidateIds).stream()
                .collect(Collectors.groupingBy(
                        InterviewPanelMemberEntity::getPanelId,
                        Collectors.mapping(InterviewPanelMemberEntity::getUserId, Collectors.toSet())
                ));
        for (InterviewPanelEntity candidate : allPanels) {
            if (excludePanelId != null && candidate.getId().equals(excludePanelId)) {
                continue;
            }
            Set<UUID> existingMembers = membersByPanel.getOrDefault(candidate.getId(), Set.of());
            if (existingMembers.equals(incoming)) {
                throw new CommonException(
                        "A panel with these exact members already exists: \"" + candidate.getName() + "\". Use the existing panel instead."
                );
            }
        }
    }

    @Transactional
    public void delete(UUID id) {
        if (panelHasSchedules(id)) {
            throw new CommonException("Cannot delete a panel that already has interview schedules.");
        }
        interviewPanelMemberRepository.deleteByPanelId(id);
        interviewPanelRepository.deleteById(id);
    }

    private boolean panelHasSchedules(UUID panelId) {
        return interviewScheduleRepository.existsByPanelId(panelId)
                || interviewRoundMetaRepository.existsByPanelId(panelId);
    }

    public List<InterviewPanelDTO> getAll() {
        List<InterviewPanelEntity> panels = interviewPanelRepository.findAllByOrderByNameAsc();
        return panels.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Drives the Manage Panels admin screen: server-side search + pagination, name ascending. */
    public Page<InterviewPanelDTO> search(String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return interviewPanelRepository.search(search, pageRequest).map(this::toDto);
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
                .hasSchedules(panelHasSchedules(panel.getId()))
                .build();
    }
}
