package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo completo de estoque via HTTP: criar, reservar, baixar e liberar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        itemRepository.deleteAll();
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private void createStock(long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/inventory").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
    }

    private static String reservation(String reference, long productId, int quantity) {
        return """
                {"reference":"%s","items":[{"productId":%d,"quantity":%d}]}
                """.formatted(reference, productId, quantity);
    }

    @Test
    void shouldReserveCommitAndUpdateStock() throws Exception {
        createStock(1, 10);

        mockMvc.perform(post("/api/inventory/reservations").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservation("order-1", 1, 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"));
        mockMvc.perform(get("/api/inventory/1").with(user()))
                .andExpect(jsonPath("$.quantityReserved").value(4))
                .andExpect(jsonPath("$.available").value(6));

        mockMvc.perform(post("/api/inventory/reservations/order-1/commit").with(user()))
                .andExpect(jsonPath("$.status").value("COMMITTED"));
        mockMvc.perform(get("/api/inventory/1").with(user()))
                .andExpect(jsonPath("$.quantityOnHand").value(6))
                .andExpect(jsonPath("$.quantityReserved").value(0));
    }

    @Test
    void shouldReleaseReservation() throws Exception {
        createStock(1, 10);
        mockMvc.perform(post("/api/inventory/reservations").with(user()).contentType(MediaType.APPLICATION_JSON)
                .content(reservation("order-2", 1, 10))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/reservations/order-2/release").with(user()))
                .andExpect(jsonPath("$.status").value("RELEASED"));
        mockMvc.perform(get("/api/inventory/1/availability?quantity=10").with(user()))
                .andExpect(jsonPath("$.sufficient").value(true));
        mockMvc.perform(post("/api/inventory/reservations/order-2/commit").with(user()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409_whenStockIsInsufficient() throws Exception {
        createStock(1, 2);

        mockMvc.perform(post("/api/inventory/reservations").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservation("order-3", 1, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient stock: product 1 requested 3, available 2"));
    }

    @Test
    void shouldAdjustStockAndValidateInput() throws Exception {
        createStock(1, 2);

        mockMvc.perform(put("/api/inventory/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityOnHand\":20}")).andExpect(jsonPath("$.available").value(20));
        mockMvc.perform(put("/api/inventory/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityOnHand\":-1}")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/inventory/1/availability?quantity=0").with(user())).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/inventory").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":5}")).andExpect(status().isConflict());
        mockMvc.perform(get("/api/inventory?page=0&size=10").with(user())).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldEnforceRoles() throws Exception {
        mockMvc.perform(post("/api/inventory").with(user()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":5}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/inventory/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/inventory/reservations/unknown").with(user())).andExpect(status().isNotFound());
    }
}
