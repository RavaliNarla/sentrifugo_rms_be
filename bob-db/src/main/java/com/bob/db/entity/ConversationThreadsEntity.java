package com.bob.db.entity;

import com.bob.db.enums.ConversationThreadsStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_threads", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.conversation_threads SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ConversationThreadsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "conversation_threads";

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "request_type_id", nullable = false)
    private UUID requestTypeId;

    @Column(name = "initiated_by", length = 255)
    private String initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 100)
    @Builder.Default
    private ConversationThreadsStatus status = ConversationThreadsStatus.PENDING;

    @Column(name = "date_extension")
    private LocalDateTime dateExtension;

    @Column(name ="zonal_id")
    private UUID zonalId;
}

