package tn.esprit.spring.clinicalservice.labRequest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "lab_request_id", nullable = false, columnDefinition = "uuid")
    private UUID labRequestId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "test_item_key", length = 120)
    private String testItemKey;

    @Column(name = "test_item_label", length = 255)
    private String testItemLabel;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;

    @Column(name = "uploaded_by", columnDefinition = "uuid")
    private UUID uploadedBy;

    @Column(name = "ai_status", length = 50)
    private String aiStatus;

    @Column(name = "ai_recommendation", length = 50)
    private String aiRecommendation;

    @Column(name = "ai_confidence", columnDefinition = "NUMERIC")
    private Double aiConfidence;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_requires_doctor_review")
    private Boolean aiRequiresDoctorReview;

    @Column(name = "extracted_creatinine_mg_dl", columnDefinition = "NUMERIC")
    private Double extractedCreatinineMgDl;

    @Column(name = "extracted_creatinine_umol", columnDefinition = "NUMERIC")
    private Double extractedCreatinineUmol;

    @Column(name = "extracted_unit", length = 50)
    private String extractedUnit;

    @Column(name = "parser_confidence", columnDefinition = "NUMERIC")
    private Double parserConfidence;

    @Column(name = "ai_raw_response", columnDefinition = "TEXT")
    private String aiRawResponse;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
