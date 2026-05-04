package tn.esprit.spring.communicationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.communicationservice.staffmessaging.controller.InternalStaffMessagingController;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffConversationType;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffMessageType;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.CreateDirectStaffConversationRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.request.SendStaffMessageRequest;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationParticipantResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffConversationSummaryResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessageResponse;
import tn.esprit.spring.communicationservice.staffmessaging.dto.response.StaffMessagingUserResponse;
import tn.esprit.spring.communicationservice.staffmessaging.service.InternalStaffMessagingService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalStaffMessagingController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalStaffMessagingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InternalStaffMessagingService internalStaffMessagingService;

    @Test
    void listUsers_shouldReturnStaffDirectory() throws Exception {
        when(internalStaffMessagingService.listAvailableStaffUsers(eq("nur"), eq(10))).thenReturn(List.of(
                StaffMessagingUserResponse.builder()
                        .userId("nurse-2")
                        .username("nurse.two")
                        .displayName("Nurse Two")
                        .role(InternalStaffRole.NURSE)
                        .build()
        ));

        mockMvc.perform(get("/api/communication/staff/users")
                        .param("q", "nur")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("nurse-2"))
                .andExpect(jsonPath("$[0].role").value("NURSE"));

        verify(internalStaffMessagingService).listAvailableStaffUsers("nur", 10);
    }

    @Test
    void createDirectConversation_shouldReturnSummary() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(internalStaffMessagingService.createOrGetDirectConversation(any(CreateDirectStaffConversationRequest.class)))
                .thenReturn(conversationSummary(conversationId));

        CreateDirectStaffConversationRequest request = new CreateDirectStaffConversationRequest();
        request.setTargetUserId("doctor-2");

        mockMvc.perform(post("/api/communication/staff/conversations/direct")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.type").value("DIRECT"));
    }

    @Test
    void sendMessage_shouldReturnMessage() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(internalStaffMessagingService.sendMessage(eq(conversationId), any(SendStaffMessageRequest.class)))
                .thenReturn(StaffMessageResponse.builder()
                        .id(UUID.randomUUID())
                        .conversationId(conversationId)
                        .senderId("doctor-1")
                        .senderRole(InternalStaffRole.DOCTOR)
                        .senderDisplayName("Doctor One")
                        .content("Please check room 12.")
                        .messageType(StaffMessageType.TEXT)
                        .createdAt(Instant.now())
                        .build());

        SendStaffMessageRequest request = new SendStaffMessageRequest();
        request.setContent("Please check room 12.");

        mockMvc.perform(post("/api/communication/staff/conversations/{conversationId}/messages", conversationId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.content").value("Please check room 12."));
    }

    @Test
    void markConversationRead_shouldReturnUpdatedSummary() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(internalStaffMessagingService.markConversationRead(conversationId)).thenReturn(conversationSummary(conversationId));

        mockMvc.perform(put("/api/communication/staff/conversations/{conversationId}/read", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }

    private StaffConversationSummaryResponse conversationSummary(UUID conversationId) {
        return StaffConversationSummaryResponse.builder()
                .id(conversationId)
                .type(StaffConversationType.DIRECT)
                .createdByUserId("doctor-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .unreadCount(0)
                .participants(List.of(
                        StaffConversationParticipantResponse.builder()
                                .userId("doctor-1")
                                .userRole(InternalStaffRole.DOCTOR)
                                .displayName("Doctor One")
                                .joinedAt(Instant.now())
                                .build()
                ))
                .build();
    }
}
