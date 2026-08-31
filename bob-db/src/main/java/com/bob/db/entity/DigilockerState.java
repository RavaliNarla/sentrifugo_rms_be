package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "digilocker_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigilockerState {

    @Id
    @Column(name = "state")
    private String state;

    @Column(name = "code_verifier", nullable = false)
    private String codeVerifier;

    @Column(name = "flow_type", nullable = false)
    private String flowType;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
