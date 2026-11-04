package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.ConflictException;

import java.util.List;

/** Nao ha estoque disponivel para um ou mais itens da reserva. HTTP 409. */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(List<String> shortages) {
        super("Insufficient stock: " + String.join("; ", shortages));
    }
}
