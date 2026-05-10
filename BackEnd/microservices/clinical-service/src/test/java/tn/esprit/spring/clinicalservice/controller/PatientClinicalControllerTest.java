package tn.esprit.spring.clinicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.clinicalservice.BaseIntegrationTest;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ClinicalAlert;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.entity.MedicalDossierEntry;
import tn.esprit.spring.clinicalservice.entity.PediatricNephrologyRecord;
import tn.esprit.spring.clinicalservice.entity.VitalSigns;
import tn.esprit.spring.clinicalservice.enums.AdmissionMode;
import tn.esprit.spring.clinicalservice.enums.AlertSeverity;
import tn.esprit.spring.clinicalservice.enums.CKDCause;
import tn.esprit.spring.clinicalservice.enums.CKDStage;
import tn.esprit.spring.clinicalservice.enums.ConsultationType;
import tn.esprit.spring.clinicalservice.enums.EntryType;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;
import tn.esprit.spring.clinicalservice.repository.MedicalDossierRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PatientClinicalControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsultationRecordRepository consultationRepository;

    @Autowired
    private MedicalDossierRepository medicalDossierRepository;

    @Autowired
    private ClinicalAlertRepository clinicalAlertRepository;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        clinicalAlertRepository.deleteAll();
        medicalDossierRepository.deleteAll();
        consultationRepository.deleteAll();
        patientId = UUID.randomUUID();
    }

    @Test
    void testGetLatestGfr_Success() throws Exception {
        consultationRepository.save(buildConsultation(patientId, LocalDateTime.of(2026, 5, 8, 9, 0), "88.4", CKDStage.STAGE_2));
        ConsultationRecord latest = consultationRepository.save(
            buildConsultation(patientId, LocalDateTime.of(2026, 5, 10, 9, 0), "54.2", CKDStage.STAGE_3A)
        );

        mockMvc.perform(get("/api/v1/patients/{patientId}/gfr", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consultationId").value(latest.getId().toString()))
            .andExpect(jsonPath("$.eGFR").value(54.2))
            .andExpect(jsonPath("$.ckdStage").value(CKDStage.STAGE_3A.name()))
            .andExpect(jsonPath("$.schwartzK").value(0.55));
    }

    @Test
    void testGetMedicalDossier_Filtered() throws Exception {
        medicalDossierRepository.save(buildDossierEntry(patientId, EntryType.CONSULTATION, "Consultation summary", LocalDateTime.of(2026, 5, 8, 10, 0)));
        medicalDossierRepository.save(buildDossierEntry(patientId, EntryType.LAB, "Lab summary", LocalDateTime.of(2026, 5, 9, 10, 0)));

        mockMvc.perform(get("/api/v1/patients/{patientId}/medical-dossier", patientId)
                .param("filters", "lab"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].entryType").value(EntryType.LAB.name()))
            .andExpect(jsonPath("$[0].summary").value("Lab summary"));
    }

    @Test
    void testGetPatientAlerts_ReturnsOnlyActiveAlerts() throws Exception {
        ClinicalAlert activeAlert = new ClinicalAlert();
        activeAlert.setPatientId(patientId);
        activeAlert.setAlertType("ELECTROLYTE_IMBALANCE");
        activeAlert.setSeverity(AlertSeverity.URGENT);
        activeAlert.setMessage("Critical hyperkalemia detected");
        activeAlert.setResolved(false);
        activeAlert.setCreatedAt(LocalDateTime.of(2026, 5, 10, 12, 0));
        clinicalAlertRepository.save(activeAlert);

        ClinicalAlert resolvedAlert = new ClinicalAlert();
        resolvedAlert.setPatientId(patientId);
        resolvedAlert.setAlertType("BP_THRESHOLD");
        resolvedAlert.setSeverity(AlertSeverity.WARNING);
        resolvedAlert.setMessage("Elevated blood pressure");
        resolvedAlert.setResolved(true);
        resolvedAlert.setCreatedAt(LocalDateTime.of(2026, 5, 9, 12, 0));
        clinicalAlertRepository.save(resolvedAlert);

        mockMvc.perform(get("/api/v1/patients/{patientId}/alerts", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].message").value("Critical hyperkalemia detected"));
    }

    @Test
    void testCreateConsultation_CreatesDossierEntry() throws Exception {
        ConsultationRecordDTO payload = ConsultationRecordDTO.builder()
            .patientId(patientId)
            .consultationType(ConsultationType.CRC.name())
            .admissionMode(AdmissionMode.SCHEDULED.name())
            .consultationDate(LocalDateTime.of(2026, 5, 10, 8, 30))
            .chiefComplaint("Edema and fatigue")
            .height_cm(new BigDecimal("125.0"))
            .weight_kg(new BigDecimal("24.5"))
            .bpSystolic_mmHg(110)
            .bpDiastolic_mmHg(70)
            .heartRate_bpm(88)
            .respiratoryRate_bpm(20)
            .temperature_C(new BigDecimal("36.8"))
            .oxygenSaturation_pct(99)
            .serumCreatinine_mgdL(new BigDecimal("0.8"))
            .ckdCause(CKDCause.CAKUT.name())
            .proteinuriaCategory("MILD")
            .hematuria("ABSENT")
            .ageYears(7)
            .isPremature(false)
            .build();

        mockMvc.perform(post("/api/v1/consultations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated());

        assertThat(medicalDossierRepository.findByPatientIdOrderByCreatedAtDesc(patientId))
            .anyMatch(entry -> entry.getEntryType() == EntryType.CONSULTATION
                && "Edema and fatigue".equals(entry.getSummary()));
    }

    private ConsultationRecord buildConsultation(UUID patientId, LocalDateTime consultationDate, String egfr, CKDStage stage) {
        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setHeight_cm(new BigDecimal("125.0"));
        vitalSigns.setWeight_kg(new BigDecimal("24.5"));
        vitalSigns.setBpSystolic_mmHg(110);
        vitalSigns.setBpDiastolic_mmHg(70);

        PediatricNephrologyRecord nephro = new PediatricNephrologyRecord();
        nephro.setEGFR(new BigDecimal(egfr));
        nephro.setSchwartz_k(new BigDecimal("0.55"));
        nephro.setCkdStage(stage);
        nephro.setCkdCause(CKDCause.CAKUT);
        nephro.setSerumCreatinine_mgdL(new BigDecimal("0.8"));

        ConsultationRecord consultation = new ConsultationRecord();
        consultation.setPatientId(patientId);
        consultation.setConsultationType(ConsultationType.CRC);
        consultation.setAdmissionMode(AdmissionMode.SCHEDULED);
        consultation.setConsultationDate(consultationDate);
        consultation.setChiefComplaint("Routine follow-up");
        consultation.setVitalSigns(vitalSigns);
        consultation.setNephologyRecord(nephro);
        consultation.setRequiresManualReview(false);
        return consultation;
    }

    private MedicalDossierEntry buildDossierEntry(UUID patientId, EntryType entryType, String summary, LocalDateTime createdAt) {
        MedicalDossierEntry entry = new MedicalDossierEntry();
        entry.setPatientId(patientId);
        entry.setEntryType(entryType);
        entry.setSummary(summary);
        entry.setCreatedAt(createdAt);
        entry.setSourceServiceId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return entry;
    }
}
