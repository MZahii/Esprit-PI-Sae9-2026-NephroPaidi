package tn.esprit.spring.pharmacyservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.spring.pharmacyservice.config.SecurityConfig;
import tn.esprit.spring.pharmacyservice.dto.*;
import tn.esprit.spring.pharmacyservice.service.StockService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
@Import(SecurityConfig.class)
@DisplayName("StockController — Integration Tests")
class StockControllerTest {

    @Autowired MockMvc        mvc;
    @Autowired ObjectMapper   json;
    @MockBean  StockService   stockService;

    // ── Security — unauthenticated requests must be 401 ──────────────────────

    @Test @DisplayName("GET /api/stock — 401 when no authentication")
    void getAllStock_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/stock"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("POST /api/stock/dispense — 401 when no authentication")
    void dispense_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/api/stock/dispense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/stock ────────────────────────────────────────────────────────

    @Test @DisplayName("GET /api/stock — 200 with PHARMACIST role")
    @WithMockUser(roles = "PHARMACIST")
    void getAllStock_pharmacist_returns200() throws Exception {
        StockDTO s = StockDTO.builder().stockId(1L).batchId(10L).quantityAvailable(50).build();
                given(stockService.getAllStock(null)).willReturn(List.of(s));

        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value(10))
                .andExpect(jsonPath("$[0].quantityAvailable").value(50));
    }

    @Test @DisplayName("GET /api/stock — 403 with PATIENT role (not in allowed list)")
    @WithMockUser(roles = "PATIENT")
    void getAllStock_patient_returns403() throws Exception {
        mvc.perform(get("/api/stock"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("GET /api/stock — 200 with GUARDIAN role (allowed by @PreAuthorize)")
    @WithMockUser(roles = "GUARDIAN")
    void getAllStock_guardian_returns200() throws Exception {
                given(stockService.getAllStock(null)).willReturn(List.of());
        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk());
    }

    // ── POST /api/stock/dispense ──────────────────────────────────────────────

    @Test @DisplayName("POST /api/stock/dispense — 200 with PHARMACIST role")
    @WithMockUser(roles = "PHARMACIST")
    void dispense_pharmacist_returns200() throws Exception {
        StockDTO result = StockDTO.builder().stockId(1L).batchId(10L).quantityAvailable(40).build();
        given(stockService.dispense(any(DispenseRequestDTO.class))).willReturn(result);

        DispenseRequestDTO req = new DispenseRequestDTO();
        req.setBatchId(10L); req.setQuantity(10);

        mvc.perform(post("/api/stock/dispense").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(40));
    }

    @Test @DisplayName("POST /api/stock/dispense — 409 on IllegalStateException (insufficient stock)")
    @WithMockUser(roles = "PHARMACIST")
    void dispense_insufficientStock_returns409() throws Exception {
        given(stockService.dispense(any())).willThrow(new IllegalStateException("Insufficient stock"));

        DispenseRequestDTO req = new DispenseRequestDTO();
        req.setBatchId(10L); req.setQuantity(999);

        mvc.perform(post("/api/stock/dispense").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ── GET /api/stock/low ────────────────────────────────────────────────────

    @Test @DisplayName("GET /api/stock/low — 200 with ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void getLowStock_admin_returns200() throws Exception {
        given(stockService.getLowStock(10)).willReturn(List.of());

        mvc.perform(get("/api/stock/low").param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── POST /api/stock/smart-dispense ────────────────────────────────────────

    @Test @DisplayName("POST /api/stock/smart-dispense — 200 returns FEFO result")
    @WithMockUser(roles = "PHARMACIST")
    void smartDispense_returns200() throws Exception {
        SmartDispenseResponseDTO resp = SmartDispenseResponseDTO.builder()
                .medicationId(1L).medicationName("Amoxicillin")
                .requested(10).totalDispensed(10).lines(List.of()).build();
        given(stockService.smartDispense(any())).willReturn(resp);

        SmartDispenseRequestDTO req = new SmartDispenseRequestDTO();
        req.setMedicationId(1L); req.setQuantity(10);

        mvc.perform(post("/api/stock/smart-dispense").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationName").value("Amoxicillin"))
                .andExpect(jsonPath("$.totalDispensed").value(10));
    }

    // ── POST /api/stock/transfer ──────────────────────────────────────────────

    @Test @DisplayName("POST /api/stock/transfer — 400 when source equals target")
    @WithMockUser(roles = "PHARMACIST")
    void transfer_sameSourceTarget_returns400() throws Exception {
        given(stockService.transferStock(any()))
                .willThrow(new IllegalArgumentException("Source and target batches must be different"));

        TransferStockRequestDTO req = new TransferStockRequestDTO();
        req.setSourceBatchId(1L); req.setTargetBatchId(1L);
        req.setQuantity(10); req.setReason("test");

        mvc.perform(post("/api/stock/transfer").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
