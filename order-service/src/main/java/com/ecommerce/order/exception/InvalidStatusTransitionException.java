package com.ecommerce.order.exception;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.order.entity.OrderStatus;

/** Transicao de status nao permitida pela maquina de estados. HTTP 409. */
public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(OrderStatus current, OrderStatus target) {
        super("Order cannot change from " + current + " to " + target
                + " (allowed: " + current.allowedTransitions() + ")");
    }
}
