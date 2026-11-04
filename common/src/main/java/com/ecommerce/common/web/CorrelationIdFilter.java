package com.ecommerce.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtro servlet que garante um Correlation ID em toda requisicao.
 *
 * <p>Se o cabecalho vier do gateway, reaproveita; caso contrario, gera um novo UUID.
 * O valor e validado para evitar "log injection" (quebras de linha ou textos enormes).</p>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_VALUE = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(CorrelationId.HEADER));
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    static String resolve(String incoming) {
        if (StringUtils.hasText(incoming) && SAFE_VALUE.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
