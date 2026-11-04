package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTOs da API de pedidos. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(
            @Schema(example = "1") @NotNull @Positive Long productId,
            @Schema(example = "2") @NotNull @Positive @Max(10_000) Integer quantity) {
    }

    /** Criacao de pedido. O preco NAO vem do cliente: e buscado no Product Service (o cliente poderia mentir). */
    public record CreateOrderRequest(
            @Schema(example = "1") @NotNull @Positive Long customerId,
            @NotEmpty @Size(max = 50) List<@Valid @NotNull OrderItemRequest> items) {
    }

    public record UpdateOrderStatusRequest(
            @Schema(example = "SHIPPED") @NotNull OrderStatus status) {
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {
    }

    public record OrderResponse(
            Long id,
            String orderNumber,
            Long customerId,
            OrderStatus status,
            BigDecimal totalAmount,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant updatedAt) {
    }
}
