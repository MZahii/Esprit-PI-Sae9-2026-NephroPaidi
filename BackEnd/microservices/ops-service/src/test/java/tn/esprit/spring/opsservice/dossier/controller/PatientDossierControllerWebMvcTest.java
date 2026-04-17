package tn.esprit.spring.opsservice.dossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.opsservice.common.dto.PagedResponse;
import tn.esprit.spring.opsservice.dossier.dto.request.AddDossierContributorRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.CreatePatientDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.DischargeDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierContributorResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierSummaryResponse;
import tn.esprit.spring.opsservice.dossier.model.AdmissionPriority;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;
import tn.esprit.spring.opsservice.dossier.service.DossierContributorService;
import tn.esprit.spring.opsservice.dossier.service.DossierService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PatientDossierController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatientDossierControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DossierService dossierService;

    @MockBean
    private DossierContributorService dossierContributorService;

    @Test
    void createShouldReturnCreatedLocation() throws Exception {
        UUID dossierId = UUID.randomUUID();
        PatientDossierResponse response = baseDossierResponse(dossierId);
        when(dossierService.createDossier(any(CreatePatientDossierRequest.class))).thenReturn(response);

        CreatePatientDossierRequest request = createRequest();

        mockMvc.perform(post("/api/ops/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/ops/dossiers/" + dossierId))
                .andExpect(jsonPath("$.id").value(dossierId.toString()));

        verify(dossierService).createDossier(any(CreatePatientDossierRequest.class));
    }

    @Test
    void listShouldWrapPagedContent() throws Exception {
        UUID dossierId = UUID.randomUUID();
        PatientDossierSummaryResponse summary = new PatientDossierSummaryResponse();
        summary.setId(dossierId);
        summary.setPatientId(9L);
        summary.setStatus(DossierStatus.ACTIVE);
        summary.setAdmissionPriority(AdmissionPriority.HIGH);
        when(dossierService.list(eq(DossierStatus.ACTIVE), eq(9L), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/ops/dossiers")
                        .param("status", "ACTIVE")
                        .param("patientId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value(9L))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(dossierService).list(DossierStatus.ACTIVE, 9L, null, null, 0, 20);
    }

    @Test
    void myAssignedShouldUseActorIdFallbackAndCallService() throws Exception {
        UUID nurseId = UUID.randomUUID();
        PatientDossierSummaryResponse summary = new PatientDossierSummaryResponse();
        summary.setId(UUID.randomUUID());
        summary.setPatientId(15L);
        summary.setStatus(DossierStatus.IN_PROGRESS);
        when(dossierService.listMyAssigned(eq(nurseId), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/ops/dossiers/my-assigned")
                        .header("X-Actor-Id", nurseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value(15L));

        verify(dossierService).listMyAssigned(nurseId, 0, 20);
    }

    @Test
    void addContributorShouldDelegateWithUserIdHeader() throws Exception {
        UUID dossierId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DossierContributorResponse response = new DossierContributorResponse();
        response.setDossierId(dossierId);
        response.setContributorId(UUID.randomUUID());
        response.setContributorRole(ContributorRole.DOCTOR);
        when(dossierContributorService.addContributor(eq(dossierId), any(AddDossierContributorRequest.class), eq(userId)))
                .thenReturn(response);

        AddDossierContributorRequest request = new AddDossierContributorRequest();
        request.setContributorId(response.getContributorId());
        request.setContributorRole(ContributorRole.DOCTOR);

        mockMvc.perform(post("/api/ops/dossiers/{dossierId}/contributors", dossierId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dossierId").value(dossierId.toString()));

        verify(dossierContributorService).addContributor(eq(dossierId), any(AddDossierContributorRequest.class), eq(userId));
    }

    @Test
    void dischargeShouldBuildActorContextFromHeaders() throws Exception {
        UUID dossierId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PatientDossierResponse response = baseDossierResponse(dossierId);
        when(dossierService.discharge(eq(dossierId), any(DischargeDossierRequest.class), any())).thenReturn(response);

        DischargeDossierRequest request = new DischargeDossierRequest();
        request.setDischargeSummary("Stable");
        request.setDischargedAt(LocalDateTime.now().plusHours(1));
        request.setRequiresSignedDischargeNote(false);

        mockMvc.perform(put("/api/ops/dossiers/{dossierId}/discharge", dossierId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", ActorRole.DOCTOR.name())
                        .header("X-User-Display-Name", "dr.amine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dossierId.toString()));

        verify(dossierService).discharge(eq(dossierId), any(DischargeDossierRequest.class), any());
    }

    @Test
    void archiveShouldDelegateToService() throws Exception {
        UUID dossierId = UUID.randomUUID();
        when(dossierService.archive(dossierId)).thenReturn(baseDossierResponse(dossierId));

        mockMvc.perform(put("/api/ops/dossiers/{dossierId}/archive", dossierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dossierId.toString()));

        verify(dossierService).archive(dossierId);
    }

    private CreatePatientDossierRequest createRequest() {
        CreatePatientDossierRequest request = new CreatePatientDossierRequest();
        request.setPatientId(9L);
        request.setSourceConsultationId(UUID.randomUUID());
        request.setSourceAppointmentId(UUID.randomUUID());
        request.setHospitalizationRequestId(UUID.randomUUID());
        request.setPrimaryDoctorId(UUID.randomUUID());
        request.setAdmissionReason("Need monitoring");
        request.setAdmissionPriority(AdmissionPriority.HIGH);
        request.setExpectedDischargeAt(LocalDateTime.now().plusDays(2));
        return request;
    }

    private PatientDossierResponse baseDossierResponse(UUID dossierId) {
        PatientDossierResponse response = new PatientDossierResponse();
        response.setId(dossierId);
        response.setPatientId(9L);
        response.setAdmissionReason("Need monitoring");
        response.setAdmissionPriority(AdmissionPriority.HIGH);
        response.setStatus(DossierStatus.ACTIVE);
        return response;
    }
}