package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.enums.OfferStatus;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.DashboardDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detail rows for dashboard metric tiles — filters match DashboardController summary counts.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRequisitionRepository jobRequisitionRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateOfferRepository candidateOfferRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;

    public DashboardDetailDTO details(String metricKey) {
        if (metricKey == null || metricKey.isBlank()) {
            throw new CommonException("Metric is required.");
        }
        String key = metricKey.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "TOTAL_REQUISITIONS" -> requisitionDetails(
                    "TOTAL_REQUISITIONS", "Total Requisitions", jobRequisitionRepository.findAll());
            case "PENDING_APPROVAL" -> requisitionDetails(
                    "PENDING_APPROVAL", "Pending Approval",
                    jobRequisitionRepository.findByStatusIn(List.of(RequisitionStatus.L1_PENDING, RequisitionStatus.L2_PENDING)));
            case "APPROVED" -> requisitionDetails(
                    "APPROVED", "Approved", jobRequisitionRepository.findByStatus(RequisitionStatus.APPROVED));
            case "FULFILLED" -> requisitionDetails(
                    "FULFILLED", "Fulfilled", jobRequisitionRepository.findByStatus(RequisitionStatus.FULFILLED));
            case "TOTAL_CANDIDATES" -> candidateDetails(
                    "TOTAL_CANDIDATES", "Total Candidates", candidateRepository.findAll());
            case "SHORTLISTED" -> candidateDetails(
                    "SHORTLISTED", "Shortlisted",
                    candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.SHORTLISTED).toList());
            case "SCHEDULED" -> candidateDetails(
                    "SCHEDULED", "Scheduled for Interview",
                    candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.SCHEDULED).toList());
            case "QUALIFIED" -> candidateDetails(
                    "QUALIFIED", "Qualified",
                    candidateRepository.findAll().stream().filter(c -> c.getStatus() == CandidateStatus.QUALIFIED).toList());
            case "OFFERS_SENT" -> offerDetails(
                    "OFFERS_SENT", "Offers Sent",
                    candidateOfferRepository.findAll().stream()
                            .filter(o -> o.getStatus() == OfferStatus.SENT || o.getStatus() == OfferStatus.ACCEPTED)
                            .toList());
            default -> throw new CommonException("Unknown dashboard metric: " + metricKey);
        };
    }

    private DashboardDetailDTO requisitionDetails(String metric, String title, List<JobRequisitionEntity> list) {
        List<String> columns = List.of("Code", "Title", "Status", "Start date", "Expected fulfilment");
        List<Map<String, String>> rows = list.stream()
                .sorted(Comparator.comparing(JobRequisitionEntity::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("Code", nullToDash(r.getRequisitionCode()));
                    row.put("Title", nullToDash(r.getTitle()));
                    row.put("Status", r.getStatus() != null ? r.getStatus().name().replace('_', ' ') : "-");
                    row.put("Start date", r.getStartDate() != null ? r.getStartDate().toString() : "-");
                    row.put("Expected fulfilment",
                            r.getExpectedFulfilmentDate() != null ? r.getExpectedFulfilmentDate().toString() : "-");
                    return row;
                })
                .collect(Collectors.toList());
        return DashboardDetailDTO.builder().metric(metric).title(title).columns(columns).rows(rows).build();
    }

    private DashboardDetailDTO candidateDetails(String metric, String title, List<CandidateEntity> list) {
        List<String> columns = List.of("Name", "Email", "Phone", "Status", "Requisition", "Position");
        Set<UUID> positionIds = list.stream().map(CandidateEntity::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, PositionContext> contexts = resolvePositionContexts(positionIds);

        List<Map<String, String>> rows = list.stream()
                .sorted(Comparator.comparing(CandidateEntity::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> {
                    PositionContext ctx = contexts.get(c.getPositionId());
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("Name", nullToDash(c.getName()));
                    row.put("Email", nullToDash(c.getEmail()));
                    row.put("Phone", nullToDash(c.getPhone()));
                    row.put("Status", c.getStatus() != null ? c.getStatus().name().replace('_', ' ') : "-");
                    row.put("Requisition", ctx != null ? nullToDash(ctx.requisition()) : "-");
                    row.put("Position", ctx != null ? nullToDash(ctx.positionTitle()) : "-");
                    return row;
                })
                .collect(Collectors.toList());
        return DashboardDetailDTO.builder().metric(metric).title(title).columns(columns).rows(rows).build();
    }

    private DashboardDetailDTO offerDetails(String metric, String title, List<CandidateOfferEntity> list) {
        List<String> columns = List.of("Candidate", "Email", "Requisition", "Position", "Status", "Accept before", "Joining date");
        Set<UUID> candidateIds = list.stream().map(CandidateOfferEntity::getCandidateId).collect(Collectors.toSet());
        Map<UUID, CandidateEntity> candidates = candidateIds.isEmpty() ? Map.of()
                : candidateRepository.findByIdIn(new ArrayList<>(candidateIds)).stream()
                .collect(Collectors.toMap(CandidateEntity::getId, c -> c, (a, b) -> a));
        Set<UUID> positionIds = candidates.values().stream()
                .map(CandidateEntity::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, PositionContext> contexts = resolvePositionContexts(positionIds);

        List<Map<String, String>> rows = list.stream()
                .sorted(Comparator.comparing(CandidateOfferEntity::getSentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(o -> {
                    CandidateEntity c = candidates.get(o.getCandidateId());
                    PositionContext ctx = c != null ? contexts.get(c.getPositionId()) : null;
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("Candidate", c != null ? nullToDash(c.getName()) : "-");
                    row.put("Email", c != null ? nullToDash(c.getEmail()) : "-");
                    row.put("Requisition", ctx != null ? nullToDash(ctx.requisition()) : "-");
                    row.put("Position", ctx != null ? nullToDash(ctx.positionTitle()) : "-");
                    String statusLabel = o.getStatus() == OfferStatus.SENT ? "OFFER LETTER SENT"
                            : (o.getStatus() != null ? o.getStatus().name().replace('_', ' ') : "-");
                    row.put("Status", statusLabel);
                    row.put("Accept before", o.getAcceptBeforeDate() != null ? o.getAcceptBeforeDate().toString() : "-");
                    row.put("Joining date", o.getJoiningDate() != null ? o.getJoiningDate().toString() : "-");
                    return row;
                })
                .collect(Collectors.toList());
        return DashboardDetailDTO.builder().metric(metric).title(title).columns(columns).rows(rows).build();
    }

    private record PositionContext(String requisition, String positionTitle) {
    }

    private Map<UUID, PositionContext> resolvePositionContexts(Set<UUID> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            return Map.of();
        }
        List<JobPositionEntity> positions = jobPositionRepository.findAllById(positionIds);
        Set<UUID> titleIds = positions.stream().map(JobPositionEntity::getPositionTitleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> requisitionIds = positions.stream().map(JobPositionEntity::getRequisitionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, String> titleNames = titleIds.isEmpty() ? Map.of()
                : positionTitleRepository.findAllById(titleIds).stream()
                .collect(Collectors.toMap(PositionTitleEntity::getId, PositionTitleEntity::getName, (a, b) -> a));
        Map<UUID, JobRequisitionEntity> requisitions = requisitionIds.isEmpty() ? Map.of()
                : jobRequisitionRepository.findAllById(requisitionIds).stream()
                .collect(Collectors.toMap(JobRequisitionEntity::getId, r -> r, (a, b) -> a));

        Map<UUID, PositionContext> result = new HashMap<>();
        for (JobPositionEntity position : positions) {
            String title = titleNames.get(position.getPositionTitleId());
            JobRequisitionEntity req = requisitions.get(position.getRequisitionId());
            String requisitionLabel = "-";
            if (req != null) {
                String code = req.getRequisitionCode();
                String reqTitle = req.getTitle();
                if (code != null && !code.isBlank() && reqTitle != null && !reqTitle.isBlank()) {
                    requisitionLabel = code + " — " + reqTitle;
                } else if (code != null && !code.isBlank()) {
                    requisitionLabel = code;
                } else if (reqTitle != null && !reqTitle.isBlank()) {
                    requisitionLabel = reqTitle;
                }
            }
            result.put(position.getId(), new PositionContext(requisitionLabel, title));
        }
        return result;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
