package tn.esprit.spring.clinicalservice.labRequest.events;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lab Result Uploaded Event
 * 
 * Published when:
 * - Lab technician uploads serum creatinine result
 * - Event is consumed by LabResultProcessingService
 * - Triggers auto-calculation of eGFR using CKD-EPI formula
 * 
 * Used for:
 * - Event-driven architecture
 * - RabbitMQ/Kafka messaging between microservices
 * - Audit trail and compliance tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LabResultUploadedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event metadata
    private UUID eventId = UUID.randomUUID();
    private LocalDateTime timestamp = LocalDateTime.now();
    private String eventType = "LabResultUploaded";
    private String version = "1.0";

    // Lab result data
    private UUID labRequestId;
    private UUID consultationId;
    private Long patientId;

    // Lab test details
    private String testName;        // e.g., "Serum Creatinine"
    private String testCode;        // e.g., "SCR", "CREATININE"
    private Double testValue;
    private String testUnit;        // e.g., "mg/dL", "µmol/L"
    private String labStandard;     // e.g., "INTERNATIONAL", "CONVENTIONAL"

    // Patient demographics (for calculation)
    private Integer patientAge;
    private String patientSex;      // "M" or "F"
    private Double patientHeight;   // in cm

    // Lab metadata
    private String labName;
    private String labCertification; // "ISO13485", "CLIA", etc.
    private String labResult;       // "POSITIVE", "NEGATIVE", or value-based
    private LocalDateTime sampleCollectionTime;
    private LocalDateTime labReceivedTime;
    private LocalDateTime resultAvailableTime;

    // Reference ranges
    private Double referenceRangeMin;
    private Double referenceRangeMax;
    private String referenceRangeUnit;

    // Additional context
    private String sourceSystem;    // "LAB_LIS", "HOSPITAL_HIS", etc.
    private boolean isAbnormal;
    private String interpretation; // "Low", "Normal", "High"

    // Audit trail
    private String uploadedBy;      // Lab technician ID
    private String approvedBy;      // Lab supervisor ID (optional)
    private LocalDateTime approvalTime;
    private String comments;
}
