/*
package com.bob.masterdata.validators;

import com.bob.db.dto.InterviewPanelsDTO;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InterviewPanelsValidator {

    private final EntityManager entityManager;

    public InterviewPanelsValidator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<String> collectValidationErrors(
            List<InterviewPanelsDTO> dtos
    ) {

        List<String> missingPanelName = new ArrayList<>();
        List<String> missingCommittee = new ArrayList<>();
        List<String> dbDuplicates = new ArrayList<>();

        for (InterviewPanelsDTO dto : dtos) {

            String key = buildKey(dto);

            if (dto.getPanelName() == null || dto.getPanelName().isBlank()) {
                missingPanelName.add(key);
            }

            if (dto.getCommitteeId() == null) {
                missingCommittee.add(key);
            }
        }

        dbDuplicates.addAll(findDbCompositeDuplicates(dtos));

        if (missingPanelName.isEmpty()
                && missingCommittee.isEmpty()
                && dbDuplicates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                buildMessage(
                        missingPanelName,
                        missingCommittee,
                        dbDuplicates
                )
        );
    }

    */
/* ================= DB COMPOSITE DUPLICATE ================= *//*


    private List<String> findDbCompositeDuplicates(
            List<InterviewPanelsDTO> dtos
    ) {

        Set<String> excelKeys =
                dtos.stream()
                        .filter(d -> d.getPanelName() != null && d.getCommitteeId() != null)
                        .map(d -> normalize(d.getPanelName()) + "|" + d.getCommitteeId())
                        .collect(Collectors.toSet());

        if (excelKeys.isEmpty()) {
            return Collections.emptyList();
        }

        String jpql =
                "SELECT LOWER(p.panelName), p.committeeId " +
                        "FROM InterviewPanelsEntity p " +
                        "WHERE p.isActive = true";

        List<Object[]> results =
                entityManager.createQuery(jpql, Object[].class)
                        .getResultList();

        List<String> duplicates = new ArrayList<>();

        for (Object[] row : results) {
            String name = (String) row[0];
            UUID committeeId = (UUID) row[1];

            String dbKey = name + "|" + committeeId;

            if (excelKeys.contains(dbKey)) {
                duplicates.add(name);
            }
        }

        return duplicates;
    }


    private String buildMessage(
            List<String> missingPanelName,
            List<String> missingCommittee,
            List<String> dbDuplicates
    ) {

        StringBuilder message = new StringBuilder();
        message.append("Interview Panel validation failed.");

        if (!missingPanelName.isEmpty()) {
            message.append("\nPanel name cannot be empty.");
        }

        if (!missingCommittee.isEmpty()) {
            message.append("\nCommittee must be selected for panels: ")
                    .append(String.join(", ", missingCommittee))
                    .append(".");
        }

        if (!dbDuplicates.isEmpty()) {
            message.append("\nInterview panels already exist in system: ")
                    .append(String.join(", ", dbDuplicates))
                    .append(".");
        }

        return message.toString();
    }

    private String buildKey(InterviewPanelsDTO dto) {
        String name =
                dto.getPanelName() != null ? dto.getPanelName() : "<unknown>";
        String committee =
                dto.getCommitteeId() != null ? dto.getCommitteeId().toString() : "<no-committee>";
        return name + "|" + committee;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }
}
*/
