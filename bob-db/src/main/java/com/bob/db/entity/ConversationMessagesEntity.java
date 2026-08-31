package com.bob.db.entity;

import com.bob.db.enums.SenderTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "conversation_messages", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.conversation_messages SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ConversationMessagesEntity extends BaseEntity<UUID> {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "sender_type", length = 100)
    @Enumerated(EnumType.STRING)
    private SenderTypeEnum senderType;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "message", columnDefinition = "text", nullable = false)
    private String message;

    @Column(name = "attachment_path", columnDefinition = "text")
    private String attachmentPath;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

}

