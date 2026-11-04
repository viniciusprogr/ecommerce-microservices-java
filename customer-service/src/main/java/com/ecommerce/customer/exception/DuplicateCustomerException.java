package com.ecommerce.customer.exception;

import com.ecommerce.common.exception.ConflictException;

/** E-mail ou CPF ja cadastrado: resulta em HTTP 409. */
public class DuplicateCustomerException extends ConflictException {

    public DuplicateCustomerException(String field) {
        super("A customer with this " + field + " already exists");
    }
}
