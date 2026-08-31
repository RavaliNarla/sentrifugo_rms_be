package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "bulk_resume_uploads", schema = "recruitment")
@NoArgsConstructor
@AllArgsConstructor
@Data
@SQLDelete(sql = "UPDATE recruitment.bulk_resume_uploads SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class BulkResumeUploadEntity  extends BaseEntity<UUID>{

    public enum Status {
        UPLOADED,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private Status status;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "original_filepath")
    private String originalFilepath;

    @Column(name = "parsed_raw_data")
    private String parsedRawData;

    @Column(name = "json_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode jsonData;

}
