package com.ecommerce.order.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Pedido inexistente. HTTP 404. */
public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Long id) {
        super("Order " + id + " not found");
    }
}
