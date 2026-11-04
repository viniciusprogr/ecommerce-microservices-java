package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Nao existe registro de estoque para o produto. HTTP 404. */
public class InventoryItemNotFoundException extends ResourceNotFoundException {

    public InventoryItemNotFoundException(Long productId) {
        super("Inventory for product " + productId + " not found");
    }
}
