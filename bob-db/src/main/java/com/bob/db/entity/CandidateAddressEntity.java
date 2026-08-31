package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "candidate_address", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_address SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateAddressEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "address_line1", columnDefinition = "text", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2", columnDefinition = "text")
    private String addressLine2;

    @Column(name = "landmark", length = 100)
    private String landmark;

    @Column(name = "state_id", nullable = false)
    private UUID stateId; // Need to check

    @Column(name = "district_id", length = 100)
    private UUID districtId;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "permanent_address_line1", columnDefinition = "text")
    private String permanentAddressLine1;

    @Column(name = "permanent_address_line2", columnDefinition = "text")
    private String permanentAddressLine2;

    @Column(name = "permanent_landmark", length = 100)
    private String permanentLandmark;

    @Column(name = "permanent_state_id")
    private UUID permanentStateId;

    @Column(name = "permanent_district_id", length = 100)
    private UUID permanentDistrictId;

    @Column(name = "permanent_city")
    private String permanentCity;

    @Column(name = "permanent_pincode", length = 10)
    private String permanentPincode;
}
