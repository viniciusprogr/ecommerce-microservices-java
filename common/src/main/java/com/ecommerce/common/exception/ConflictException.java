package com.ecommerce.common.exception;

/**
 * Conflito com o estado atual do recurso (duplicidade, estoque insuficiente,
 * transicao de estado invalida). Mapeada para HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
