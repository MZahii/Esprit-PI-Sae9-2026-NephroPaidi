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
import tn.esprit.spring.pharmacyservice.service.SupplierService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
@Import(SecurityConfig.class)
@DisplayName("SupplierController — Integration Tests")
class SupplierControllerTest {

    @Autowired MockMvc       mvc;
    @Autowired ObjectMapper  json;
    @MockBean  SupplierService supplierService;

    // ── Security ──────────────────────────────────────────────────────────────

    @Test @DisplayName("GET /api/suppliers — 401 without authentication")
    void getAll_unauthenticated_401() throws Exception {
        mvc.perform(get("/api/suppliers"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("DELETE /api/suppliers/1 — 403 with PHARMACIST role (only ADMIN allowed)")
    @WithMockUser(roles = "PHARMACIST")
    void delete_pharmacist_403() throws Exception {
        mvc.perform(delete("/api/suppliers/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("DELETE /api/suppliers/1 — 204 with ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void delete_admin_204() throws Exception {
        willDoNothing().given(supplierService).deleteSupplier(1L);

        mvc.perform(delete("/api/suppliers/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/suppliers ────────────────────────────────────────────────────

    @Test @DisplayName("GET /api/suppliers — 200 returns supplier list")
    @WithMockUser(roles = "PHARMACIST")
    void getAll_200() throws Exception {
        SupplierDTO s = SupplierDTO.builder()
                .supplierId(1L).name("PharmaCo").contactInfo("pharma@co.com").isActive(true).build();
        given(supplierService.getAllSuppliers(null, null)).willReturn(List.of(s));

        mvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("PharmaCo"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    // ── POST /api/suppliers ───────────────────────────────────────────────────

    @Test @DisplayName("POST /api/suppliers — 201 with PHARMACIST role")
    @WithMockUser(roles = "PHARMACIST")
    void create_201() throws Exception {
        SupplierDTO created = SupplierDTO.builder()
                .supplierId(1L).name("NewSupplier").isActive(true).build();
        given(supplierService.createSupplier(any())).willReturn(created);

        SupplierDTO req = new SupplierDTO();
        req.setName("NewSupplier"); req.setContactInfo("contact@new.com");

        mvc.perform(post("/api/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(1))
                .andExpect(jsonPath("$.name").value("NewSupplier"));
    }

    // ── PATCH /api/suppliers/{id}/toggle-status ───────────────────────────────

    @Test @DisplayName("PATCH /api/suppliers/1/toggle-status — 200 toggles active flag")
    @WithMockUser(roles = "ADMIN")
    void toggleStatus_200() throws Exception {
        SupplierDTO toggled = SupplierDTO.builder()
                .supplierId(1L).name("PharmaCo").isActive(false).build();
        given(supplierService.toggleStatus(1L)).willReturn(toggled);

        mvc.perform(patch("/api/suppliers/1/toggle-status").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    // ── GET /api/suppliers/{id}/stats ─────────────────────────────────────────

    @Test @DisplayName("GET /api/suppliers/1/stats — 200 returns performance stats")
    @WithMockUser(roles = "PHARMACIST")
    void getStats_200() throws Exception {
        SupplierStatsDTO stats = SupplierStatsDTO.builder()
                .supplierId(1L).supplierName("PharmaCo")
                .totalOrders(10).deliveredOrders(8)
                .pendingOrders(2).cancelledOrders(0)
                .overdueOrders(1).deliveryRate(80.0).build();
        given(supplierService.getStats(1L)).willReturn(stats);

        mvc.perform(get("/api/suppliers/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryRate").value(80.0))
                .andExpect(jsonPath("$.overdueOrders").value(1));
    }

    // ── POST /api/suppliers/{id}/orders ──────────────────────────────────────

    @Test @DisplayName("POST /api/suppliers/1/orders — 409 when supplier is inactive")
    @WithMockUser(roles = "PHARMACIST")
    void placeOrder_inactiveSupplier_409() throws Exception {
        given(supplierService.placeOrder(eq(1L), any()))
                .willThrow(new IllegalStateException("Cannot place orders for an inactive supplier"));

        SupplyOrderDTO req = new SupplyOrderDTO();
        req.setMedicationId(10L); req.setOrderedQuantity(5);

        mvc.perform(post("/api/suppliers/1/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ── PATCH /api/suppliers/orders/{id}/cancel ───────────────────────────────

    @Test @DisplayName("PATCH cancel — 409 when order already delivered")
    @WithMockUser(roles = "PHARMACIST")
    void cancelDelivered_409() throws Exception {
        given(supplierService.cancelOrder(5L))
                .willThrow(new IllegalStateException("Cannot cancel an already delivered order"));

        mvc.perform(patch("/api/suppliers/orders/5/cancel").with(csrf()))
                .andExpect(status().isConflict());
    }
}
