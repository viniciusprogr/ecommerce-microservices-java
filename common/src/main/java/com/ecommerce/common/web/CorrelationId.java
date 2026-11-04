package com.ecommerce.common.web;

import org.slf4j.MDC;

/**
 * Constantes e utilitarios do Correlation ID.
 *
 * <p>O Correlation ID e um identificador unico gerado na borda (API Gateway) e repassado
 * em todas as chamadas entre servicos pelo cabecalho {@value #HEADER}. Ele e colocado no
 * MDC (Mapped Diagnostic Context) do SLF4J, entao aparece em todas as linhas de log da
 * requisicao, permitindo rastrear um pedido de ponta a ponta.</p>
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    /**
     * @return o correlation id da requisicao atual, ou {@code null} fora de uma requisicao
     */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
