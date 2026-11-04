package com.ecommerce.order.client;

import com.ecommerce.common.exception.RemoteServiceException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Traducao padronizada de falhas tecnicas de chamadas HTTP.
 *
 * <p>Erros "esperados" (404, 409) sao tratados por cada cliente com {@code onStatus}. Aqui ficam os erros
 * inesperados: servico fora do ar/timeout ({@link ResourceAccessException}) e qualquer outro status de erro.
 * Todos viram {@link RemoteServiceException} (HTTP 503 para quem chamou o Order Service).</p>
 */
final class RemoteCalls {

    private RemoteCalls() {
    }

    static <T> T execute(String serviceName, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException ex) {
            throw new RemoteServiceException(serviceName + " is unavailable", ex);
        } catch (RestClientResponseException ex) {
            throw new RemoteServiceException(serviceName + " responded with status " + ex.getStatusCode().value(), ex);
        }
    }
}
