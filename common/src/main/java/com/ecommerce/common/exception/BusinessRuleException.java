package com.ecommerce.common.exception;

/**
 * Requisicao sintaticamente valida, mas que viola uma regra de negocio.
 * Mapeada para HTTP 422 (Unprocessable Content).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
