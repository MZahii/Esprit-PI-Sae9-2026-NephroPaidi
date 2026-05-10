package tn.esprit.spring.clinicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.clinicalservice.BaseIntegrationTest;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.enums.ConsultationType;
import tn.esprit.spring.clinicalservice.enums.AdmissionMode;
import tn.esprit.spring.clinicalservice.mapper.ConsultationRecordMapper;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ConsultationController
 * Tests CRUD endpoints and consultation-related operations
 */
public class ConsultationControllerTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ConsultationRecordRepository consultationRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    private UUID patientId;
    private UUID consultationId;
    
    @BeforeEach
    public void setUp() {
        consultationRepository.deleteAll();
        patientId = UUID.randomUUID();
    }
    
    /**
     * Test: POST /api/v1/consultations - Create new consultation
     * Expected: 201 CREATED with consultation data
     */
    @Test
    public void testCreateConsultation_Success() throws Exception {
        ConsultationRecordDTO payload = buildDto(patientId, LocalDateTime.of(2026, 5, 10, 8, 30));

        mockMvc.perform(post("/api/v1/consultations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.consultationType").value(ConsultationType.CRC.name()))
            .andExpect(jsonPath("$.admissionMode").value(AdmissionMode.SCHEDULED.name()))
            .andExpect(jsonPath("$.chiefComplaint").value("Fatigue and edema"))
            .andExpect(jsonPath("$.contentType").value("application/pdf"))
            .andExpect(jsonPath("$.requiresManualReview").value(false));

        List<ConsultationRecord> records = consultationRepository.findByPatientIdOrderByConsultationDateDesc(patientId);
        assertThat(records).hasSize(1);
        ConsultationRecord saved = records.get(0);
        consultationId = saved.getId();
        assertThat(saved.getConsultationType()).isEqualTo(ConsultationType.CRC);
        assertThat(saved.getAdmissionMode()).isEqualTo(AdmissionMode.SCHEDULED);
        assertThat(saved.getChiefComplaint()).isEqualTo("Fatigue and edema");
    }
    
    /**
     * Test: GET /api/v1/consultations/{id} - Get consultation by ID
     * Expected: 200 OK with consultation data
     */
    @Test
    public void testGetConsultationById_Success() throws Exception {
        ConsultationRecord saved = consultationRepository.save(buildEntity(patientId, LocalDateTime.of(2026, 5, 9, 10, 0), "Routine follow-up"));
        consultationId = saved.getId();

        mockMvc.perform(get("/api/v1/consultations/{id}", consultationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(consultationId.toString()))
            .andExpect(jsonPath("$.patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.consultationType").value(ConsultationType.CRC.name()))
            .andExpect(jsonPath("$.admissionMode").value(AdmissionMode.SCHEDULED.name()))
            .andExpect(jsonPath("$.chiefComplaint").value("Routine follow-up"));
    }
    
    /**
     * Test: GET /api/v1/consultations/{id} - Consultation not found
     * Expected: 404 NOT FOUND
     */
    @Test
    public void testGetConsultationById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }
    
    /**
     * Test: GET /api/v1/consultations?patientId={id} - Get all consultations for patient
     * Expected: 200 OK with list of consultations
     */
    @Test
    public void testGetConsultationsByPatient_Success() throws Exception {
        consultationRepository.save(buildEntity(patientId, LocalDateTime.of(2026, 5, 8, 9, 0), "Older consultation"));
        consultationRepository.save(buildEntity(patientId, LocalDateTime.of(2026, 5, 10, 11, 0), "Latest consultation"));
        consultationRepository.save(buildEntity(UUID.randomUUID(), LocalDateTime.of(2026, 5, 11, 12, 0), "Other patient"));

        mockMvc.perform(get("/api/v1/consultations")
                .param("patientId", patientId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].chiefComplaint").value("Latest consultation"))
            .andExpect(jsonPath("$[1].chiefComplaint").value("Older consultation"));
    }
    
    /**
     * Test: PUT /api/v1/consultations/{id} - Update consultation
     * Expected: 200 OK with updated data
     */
    @Test
    public void testUpdateConsultation_Success() throws Exception {
        ConsultationRecord saved = consultationRepository.save(buildEntity(patientId, LocalDateTime.of(2026, 5, 9, 14, 0), "Initial complaint"));
        consultationId = saved.getId();

        ConsultationRecordDTO payload = buildDto(patientId, LocalDateTime.of(2026, 5, 10, 15, 0));
        payload.setChiefComplaint("Updated complaint");
        payload.setConsultationType(ConsultationType.CRH.name());
        payload.setAdmissionMode(AdmissionMode.EMERGENCY.name());
        payload.setParserConfidence(0.93f);

        mockMvc.perform(put("/api/v1/consultations/{id}", consultationId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(consultationId.toString()))
            .andExpect(jsonPath("$.chiefComplaint").value("Updated complaint"))
            .andExpect(jsonPath("$.consultationType").value(ConsultationType.CRH.name()))
            .andExpect(jsonPath("$.admissionMode").value(AdmissionMode.EMERGENCY.name()))
            .andExpect(jsonPath("$.parserConfidence").value(0.93));

        ConsultationRecord updated = consultationRepository.findById(consultationId).orElseThrow();
        assertThat(updated.getChiefComplaint()).isEqualTo("Updated complaint");
        assertThat(updated.getConsultationType()).isEqualTo(ConsultationType.CRH);
        assertThat(updated.getAdmissionMode()).isEqualTo(AdmissionMode.EMERGENCY);
    }
    
    /**
     * Test: DELETE /api/v1/consultations/{id} - Delete consultation
     * Expected: 204 NO CONTENT
     */
    @Test
    public void testDeleteConsultation_Success() throws Exception {
        ConsultationRecord saved = consultationRepository.save(buildEntity(patientId, LocalDateTime.of(2026, 5, 9, 16, 0), "Delete me"));
        consultationId = saved.getId();

        mockMvc.perform(delete("/api/v1/consultations/{id}", consultationId))
            .andExpect(status().isNoContent());

        assertThat(consultationRepository.existsById(consultationId)).isFalse();
    }

    private ConsultationRecordDTO buildDto(UUID patientId, LocalDateTime consultationDate) {
        return ConsultationRecordDTO.builder()
            .patientId(patientId)
            .consultationType(ConsultationType.CRC.name())
            .admissionMode(AdmissionMode.SCHEDULED.name())
            .consultationDate(consultationDate)
            .chiefComplaint("Fatigue and edema")
            .weight_kg(new BigDecimal("24.5"))
            .height_cm(new BigDecimal("125.0"))
            .bpSystolic_mmHg(110)
            .bpDiastolic_mmHg(70)
            .heartRate_bpm(88)
            .respiratoryRate_bpm(20)
            .temperature_C(new BigDecimal("36.8"))
            .oxygenSaturation_pct(99)
            .eGFR(new BigDecimal("85.4"))
            .ckdStage("STAGE_1")
            .ckdCause("CAKUT")
            .serumCreatinine_mgdL(new BigDecimal("0.8"))
            .proteinuriaCategory("MILD")
            .hematuria("ABSENT")
            .serumAlbumin_g_L(new BigDecimal("42.0"))
            .serumPotassium_mmolL(new BigDecimal("4.3"))
            .parserConfidence(0.84f)
            .contentType("application/pdf")
            .requiresManualReview(false)
            .documentAgeDays(3)
            .isReviewed(false)
            .hospitalId(1)
            .departmentId(2)
            .serumCreatinine_umolL(new BigDecimal("70.7"))
            .documentQualityScore(92.0f)
            .build();
    }

    private ConsultationRecord buildEntity(UUID patientId, LocalDateTime consultationDate, String chiefComplaint) {
        ConsultationRecord entity = ConsultationRecordMapper.INSTANCE.toEntity(buildDto(patientId, consultationDate));
        entity.setChiefComplaint(chiefComplaint);
        return entity;
    }
}
