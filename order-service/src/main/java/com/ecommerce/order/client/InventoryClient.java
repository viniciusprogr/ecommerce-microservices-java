package com.ecommerce.order.client;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.order.client.RemoteDtos.ReservationRequest;
import com.ecommerce.order.client.RemoteDtos.ReservationResponse;
import com.ecommerce.order.exception.InsufficientStockException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/** Cliente HTTP da API interna de reservas do Inventory Service. */
public class InventoryClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public InventoryClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Reserva todos os itens (ou nenhum).
     *
     * @throws InsufficientStockException se o Inventory responder 409
     */
    public ReservationResponse reserve(ReservationRequest request) {
        return RemoteCalls.execute("inventory-service", () -> restClient.post()
                .uri("/api/inventory/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (req, response) -> {
                    throw new InsufficientStockException(messageOf(response, "Insufficient stock"));
                })
                .body(ReservationResponse.class));
    }

    /** Baixa definitiva do estoque (pedido enviado). */
    public ReservationResponse commit(String reference) {
        return changeReservation(reference, "commit");
    }

    /** Libera o estoque reservado (pedido cancelado ou compensacao). */
    public ReservationResponse release(String reference) {
        return changeReservation(reference, "release");
    }

    private ReservationResponse changeReservation(String reference, String action) {
        return RemoteCalls.execute("inventory-service", () -> restClient.post()
                .uri("/api/inventory/reservations/{reference}/{action}", reference, action)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (req, response) -> {
                    throw new ConflictException(messageOf(response, "Cannot " + action + " reservation " + reference));
                })
                .body(ReservationResponse.class));
    }

    /** Extrai o campo "message" do ApiError retornado pelo Inventory, se existir. */
    private String messageOf(ClientHttpResponse response, String fallback) {
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            JsonNode message = body == null ? null : body.get("message");
            return message != null && message.isTextual() ? message.asText() : fallback;
        } catch (IOException ex) {
            return fallback;
        }
    }
}
