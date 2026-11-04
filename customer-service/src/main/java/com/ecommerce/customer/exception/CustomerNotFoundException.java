package com.ecommerce.customer.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Cliente inexistente: resulta em HTTP 404. */
public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(Long id) {
        super("Customer " + id + " not found");
    }
}
