package com.ecommerce.order.event;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de dominio do pedido. Imutavel (record) e autocontido: quem consumir o evento
 * nao precisa consultar o Order Service para entende-lo.
 *
 * @param type        tipo do evento
 * @param orderId     id interno do pedido
 * @param orderNumber identificador de negocio
 * @param customerId  cliente
 * @param status      status apos o evento
 * @param totalAmount valor total
 * @param occurredAt  momento do evento
 */
public record OrderEvent(
        Type type,
        Long orderId,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant occurredAt) {

    /** Tipos de evento publicados. */
    public enum Type {
        ORDER_CREATED,
        ORDER_STATUS_CHANGED
    }

    public static OrderEvent of(Type type, Order order) {
        return new OrderEvent(type, order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), Instant.now());
    }
}
