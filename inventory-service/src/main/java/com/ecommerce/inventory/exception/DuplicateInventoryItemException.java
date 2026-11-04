package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.ConflictException;

/** Ja existe registro de estoque para o produto. HTTP 409. */
public class DuplicateInventoryItemException extends ConflictException {

    public DuplicateInventoryItemException(Long productId) {
        super("Inventory for product " + productId + " already exists");
    }
}
