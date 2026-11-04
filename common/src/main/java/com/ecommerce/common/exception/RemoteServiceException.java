package com.ecommerce.common.exception;

/**
 * Falha de comunicacao com outro microsservico (indisponivel, timeout, erro 5xx).
 * Mapeada para HTTP 503, sinalizando que o problema e temporario.
 */
public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteServiceException(String message) {
        super(message);
    }
}
