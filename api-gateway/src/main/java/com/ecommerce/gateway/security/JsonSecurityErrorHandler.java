package com.ecommerce.gateway.security;

import com.ecommerce.gateway.filter.CorrelationIdFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Respostas 401/403 em JSON, no mesmo formato de erro dos servicos.
 * A mensagem e generica para nao revelar o motivo da falha (expirado, assinatura invalida...).
 */
@Component
public class JsonSecurityErrorHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer");
        return write(exchange, HttpStatus.UNAUTHORIZED, "Authentication required: provide a valid Bearer token");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return write(exchange, HttpStatus.FORBIDDEN, "Access denied");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER);
        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","correlationId":"%s"}"""
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(), message,
                        exchange.getRequest().getPath().value(), correlationId == null ? "" : correlationId);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
