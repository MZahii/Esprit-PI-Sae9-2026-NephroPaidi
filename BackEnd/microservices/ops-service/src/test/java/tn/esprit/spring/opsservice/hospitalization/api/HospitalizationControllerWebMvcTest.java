package tn.esprit.spring.opsservice.hospitalization.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationTaskRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationCaseResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationSummaryResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskUpdateRequest;
import tn.esprit.spring.opsservice.hospitalization.application.HospitalizationService;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationMeasurementKind;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HospitalizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class HospitalizationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HospitalizationService hospitalizationService;

    @Test
    void createHospitalizationShouldReturnMappedResponse() throws Exception {
        UUID caseId = UUID.randomUUID();
        HospitalizationCaseResponse response = new HospitalizationCaseResponse(
                caseId,
                11L,
                UUID.randomUUID(),
                "doctor-sub",
                "dr.amine",
                "Need observation",
                HospitalizationStatus.REQUESTED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of()
        );
        when(hospitalizationService.createHospitalization(any(CreateHospitalizationRequest.class))).thenReturn(response);

        CreateHospitalizationRequest request = new CreateHospitalizationRequest(
                11L,
                UUID.randomUUID(),
                "Need observation",
                List.of(new CreateHospitalizationTaskRequest(
                        HospitalizationTaskType.BLOOD_MONITORING,
                        "Vitals",
                        "Check vitals",
                        HospitalizationMeasurementKind.TEXT,
                        null,
                        null
                ))
        );

        mockMvc.perform(post("/api/hospitalizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId.toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        verify(hospitalizationService).createHospitalization(any(CreateHospitalizationRequest.class));
    }

    @Test
    void addTaskShouldDelegateToService() throws Exception {
        UUID hospitalizationId = UUID.randomUUID();
        HospitalizationTaskResponse response = new HospitalizationTaskResponse(
                UUID.randomUUID(),
                HospitalizationTaskType.BLOOD_MONITORING,
                "Vitals",
                "Check vitals",
                HospitalizationMeasurementKind.NUMERIC,
                "mmHg",
                0,
                HospitalizationTaskStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
        when(hospitalizationService.addTask(eq(hospitalizationId), any(CreateHospitalizationTaskRequest.class))).thenReturn(response);

        CreateHospitalizationTaskRequest request = new CreateHospitalizationTaskRequest(
                HospitalizationTaskType.BLOOD_MONITORING,
                "Vitals",
                "Check vitals",
                HospitalizationMeasurementKind.NUMERIC,
                "mmHg",
                0
        );

        mockMvc.perform(post("/api/hospitalizations/{hospitalizationId}/tasks", hospitalizationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vitals"));

        verify(hospitalizationService).addTask(eq(hospitalizationId), any(CreateHospitalizationTaskRequest.class));
    }

    @Test
    void nurseActiveEndpointShouldReturnSummaryList() throws Exception {
        when(hospitalizationService.getActiveHospitalizationsForNurse()).thenReturn(List.of(
                new HospitalizationSummaryResponse(
                        UUID.randomUUID(),
                        22L,
                        UUID.randomUUID(),
                        "dr.sara",
                        "Monitoring",
                        HospitalizationStatus.ACTIVE,
                        3,
                        2,
                        1,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        ));

        mockMvc.perform(get("/api/nurse/hospitalizations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(22L))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(hospitalizationService).getActiveHospitalizationsForNurse();
    }

    @Test
    void updateTaskShouldDelegateToService() throws Exception {
        UUID taskId = UUID.randomUUID();
        HospitalizationTaskResponse response = new HospitalizationTaskResponse(
                taskId,
                HospitalizationTaskType.BLOOD_MONITORING,
                "Vitals",
                "Check vitals",
                HospitalizationMeasurementKind.NUMERIC,
                "mmHg",
                0,
                HospitalizationTaskStatus.DONE,
                "Completed",
                new BigDecimal("118"),
                null,
                "mmHg",
                "nurse-1",
                "nurse.amina",
                LocalDateTime.now(),
                List.of()
        );
        when(hospitalizationService.recordTaskExecution(eq(taskId), any(HospitalizationTaskUpdateRequest.class))).thenReturn(response);

        HospitalizationTaskUpdateRequest request = new HospitalizationTaskUpdateRequest(
                HospitalizationTaskStatus.DONE,
                "Completed",
                new BigDecimal("118"),
                null,
                null
        );

        mockMvc.perform(put("/api/nurse/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.latestNumericValue").value(118));

        verify(hospitalizationService).recordTaskExecution(eq(taskId), any(HospitalizationTaskUpdateRequest.class));
    }
}