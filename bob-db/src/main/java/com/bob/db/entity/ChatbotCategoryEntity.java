package com.bob.db.entity;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;
import org.hibernate.annotations.*;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Data
@Table(name = "chatbot_category", schema = "recruitment")
@SQLDelete(sql = "UPDATE recruitment.chatbot_category SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ChatbotCategoryEntity  extends BaseEntity<UUID>{

    @Column(name = "parent_category", nullable = false)
    private String parentCategory;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "answer", nullable = false)
    private String answer;


//    @Column(name = "is_active")
//    private Boolean isActive;
}
