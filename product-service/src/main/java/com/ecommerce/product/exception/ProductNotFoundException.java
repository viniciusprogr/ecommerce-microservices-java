package com.ecommerce.product.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Produto inexistente: HTTP 404. */
public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Long id) {
        super("Product " + id + " not found");
    }
}
