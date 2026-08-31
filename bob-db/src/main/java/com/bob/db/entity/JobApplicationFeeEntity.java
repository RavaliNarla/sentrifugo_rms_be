package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "job_application_fee", schema = "recruitment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_application_fee SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobApplicationFeeEntity  extends BaseEntity<UUID>{

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "application_fee_id", nullable = false)
    private Integer applicationFeeId;

}
