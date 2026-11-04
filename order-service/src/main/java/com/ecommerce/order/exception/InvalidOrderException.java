package com.ecommerce.order.exception;

import com.ecommerce.common.exception.BusinessRuleException;

/** Pedido invalido: cliente/produto inexistente ou inativo, produto repetido. HTTP 422. */
public class InvalidOrderException extends BusinessRuleException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
