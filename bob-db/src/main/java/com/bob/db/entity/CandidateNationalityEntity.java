package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "candidate_nationality", schema = "recruitment")
@Data
@SQLDelete(sql = "UPDATE recruitment.candidate_nationality SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateNationalityEntity  extends BaseEntity<UUID>{

    @Column(name = "position_id")
    private UUID positionId;

    @Column(name = "country_id")
    private int countryId;

}
