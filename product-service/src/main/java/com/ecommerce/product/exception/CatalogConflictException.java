package com.ecommerce.product.exception;

import com.ecommerce.common.exception.ConflictException;

/** Conflitos do catalogo: SKU ou nome de categoria duplicado, categoria em uso. HTTP 409. */
public class CatalogConflictException extends ConflictException {

    public CatalogConflictException(String message) {
        super(message);
    }
}
