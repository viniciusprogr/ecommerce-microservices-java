package com.ecommerce.product.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Categoria inexistente: HTTP 404. */
public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(Long id) {
        super("Category " + id + " not found");
    }
}
