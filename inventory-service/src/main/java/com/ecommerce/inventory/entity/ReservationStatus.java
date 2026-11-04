package com.ecommerce.inventory.entity;

/**
 * Ciclo de vida de uma reserva.
 *
 * <pre>
 * RESERVED --commit--> COMMITTED  (baixa definitiva)
 * RESERVED --release-> RELEASED   (estoque devolvido)
 * </pre>
 */
public enum ReservationStatus {
    RESERVED,
    COMMITTED,
    RELEASED
}
