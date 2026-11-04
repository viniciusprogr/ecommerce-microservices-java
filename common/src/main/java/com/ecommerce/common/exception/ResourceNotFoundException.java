package com.ecommerce.common.exception;

/**
 * Recurso solicitado nao existe. Mapeada para HTTP 404.
 *
 * <p>Cada servico cria subclasses especificas do seu dominio
 * (ex.: {@code CustomerNotFoundException}) para deixar o codigo mais expressivo.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
