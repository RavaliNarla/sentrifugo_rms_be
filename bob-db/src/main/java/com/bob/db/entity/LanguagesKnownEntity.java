package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "languages_known", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@SQLDelete(sql = "UPDATE candidate.languages_known SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class LanguagesKnownEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "language_id", length = 255, nullable = false)
    private UUID languageId;

    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private Boolean canRead = false;

    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private Boolean canWrite = false;

    @Column(name = "can_speak", nullable = false)
    @Builder.Default
    private Boolean canSpeak = false;
}
