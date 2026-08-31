package com.bob.db.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@Entity
@Table(name = "exam_centres", schema = "common")
@SQLDelete(sql = "UPDATE common.exam_centres SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ExamCenterEntity extends BaseEntity<UUID> {

    @Column(name = "exam_centre")
    private String examCentre;

}
