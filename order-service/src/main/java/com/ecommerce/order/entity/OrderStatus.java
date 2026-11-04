package com.ecommerce.order.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status do pedido e suas transicoes permitidas (maquina de estados).
 *
 * <pre>
 * CONFIRMED -> SHIPPED -> DELIVERED
 * CONFIRMED -> CANCELLED
 * </pre>
 *
 * <p>Colocar as regras no proprio enum deixa a maquina de estados explicita, facil de testar
 * e impossivel de "esquecer" em algum service.</p>
 */
public enum OrderStatus {
    /** Criado com estoque reservado. */
    CONFIRMED,
    /** Enviado: estoque baixado definitivamente. */
    SHIPPED,
    /** Entregue ao cliente. Estado final. */
    DELIVERED,
    /** Cancelado: reserva liberada. Estado final. */
    CANCELLED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case CONFIRMED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }
}
