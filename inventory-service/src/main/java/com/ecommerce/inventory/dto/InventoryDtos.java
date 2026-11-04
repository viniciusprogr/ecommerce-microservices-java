package com.ecommerce.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs do Inventory Service agrupados como records aninhados.
 *
 * <p>Como sao pequenos e fortemente relacionados, mante-los juntos facilita a leitura.
 * Cada record continua sendo um tipo independente: {@code InventoryDtos.ReservationRequest}.</p>
 */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    /** Cria o registro de estoque de um produto. */
    public record CreateInventoryItemRequest(
            @Schema(example = "1") @NotNull @Positive Long productId,
            @Schema(example = "100") @NotNull @PositiveOrZero @Max(1_000_000) Integer quantity) {
    }

    /** Ajuste manual da quantidade fisica. */
    public record StockAdjustmentRequest(
            @Schema(example = "150") @NotNull @PositiveOrZero @Max(1_000_000) Integer quantityOnHand) {
    }

    public record InventoryItemResponse(
            Long productId,
            int quantityOnHand,
            int quantityReserved,
            int available,
            Instant updatedAt) {
    }

    public record AvailabilityResponse(Long productId, int requested, int available, boolean sufficient) {
    }

    public record ReservationItem(
            @Schema(example = "1") @NotNull @Positive Long productId,
            @Schema(example = "2") @NotNull @Positive @Max(10_000) Integer quantity) {
    }

    /** Pedido de reserva; {@code reference} e a chave de idempotencia (numero do pedido). */
    public record ReservationRequest(
            @Schema(example = "3f6c1a7e-2b1d-4c8e-9f0a-1b2c3d4e5f60") @NotBlank @Size(max = 64) String reference,
            @NotEmpty @Size(max = 100) List<@Valid @NotNull ReservationItem> items) {
    }

    public record ReservationLineResponse(Long productId, int quantity) {
    }

    public record ReservationResponse(
            String reference,
            String status,
            List<ReservationLineResponse> items,
            Instant createdAt,
            Instant updatedAt) {
    }
}
