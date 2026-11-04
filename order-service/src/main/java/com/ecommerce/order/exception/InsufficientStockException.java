package com.ecommerce.order.exception;

import com.ecommerce.common.exception.ConflictException;

/** O Inventory Service recusou a reserva por falta de estoque. HTTP 409. */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
