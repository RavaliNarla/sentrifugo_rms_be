package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@Entity
@Table(name = "rec_generic_documents", schema = "recruitment")
@SQLDelete(sql = "UPDATE recruitment.rec_generic_documents SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecGenericDocumentsEntity extends BaseEntity<UUID> {

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "display_name")
    private String displayName;
}

