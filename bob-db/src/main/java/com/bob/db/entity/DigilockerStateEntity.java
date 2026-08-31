package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "digilocker_state", schema = "candidate")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DigilockerStateEntity extends BaseEntity<UUID> {

    @Column(name = "state", unique = true, nullable = false)
    private String state;

    @Column(name = "code_verifier", nullable = false)
    private String codeVerifier;

    @Column(name = "flow_type", nullable = false)
    private String flowType;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "doc_code", length = 20)
    private String docCode;

    @Column(name = "page_number", columnDefinition = "int4 DEFAULT 0")
    private Integer pageNumber;
}
