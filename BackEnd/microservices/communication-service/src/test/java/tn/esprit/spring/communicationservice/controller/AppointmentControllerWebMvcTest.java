package tn.esprit.spring.communicationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;
import tn.esprit.spring.communicationservice.dto.request.ApproveAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;
import tn.esprit.spring.communicationservice.service.AppointmentRequestService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentRequestService appointmentRequestService;

    @MockBean
    private GuardianPatientResolverService guardianPatientResolverService;

    @Test
    void approve_shouldReturnMappedResponse() throws Exception {
        UUID requestId = UUID.randomUUID();
        LocalDateTime scheduledDate = LocalDateTime.now().plusDays(1);

        AppointmentRequestResponse response = AppointmentRequestResponse.builder()
                .id(requestId)
                .patientId(4L)
                .status(AppointmentStatus.APPROVED)
                .scheduledDate(scheduledDate)
                .reason("follow-up")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(appointmentRequestService.approve(eq(requestId), any(ApproveAppointmentRequest.class))).thenReturn(response);

        ApproveAppointmentRequest payload = new ApproveAppointmentRequest();
        payload.setScheduledDate(scheduledDate);
        payload.setReceptionistNotes("validated");

        mockMvc.perform(post("/api/appointments/requests/{id}/approve", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(appointmentRequestService).approve(eq(requestId), any(ApproveAppointmentRequest.class));
    }

    @Test
    void myPatients_shouldDelegateToResolver() throws Exception {
        when(guardianPatientResolverService.getMyPatients()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/appointments/my-patients"))
                .andExpect(status().isOk());

        verify(guardianPatientResolverService).getMyPatients();
    }
}
