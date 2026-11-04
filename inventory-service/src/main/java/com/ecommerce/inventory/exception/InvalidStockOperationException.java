package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.BusinessRuleException;

/** Operacao de estoque que violaria uma invariante (quantidade negativa, liberar mais que o reservado). HTTP 422. */
public class InvalidStockOperationException extends BusinessRuleException {

    public InvalidStockOperationException(String message) {
        super(message);
    }
}
