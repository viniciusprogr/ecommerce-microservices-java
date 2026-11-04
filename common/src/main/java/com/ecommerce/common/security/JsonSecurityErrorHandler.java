package com.ecommerce.common.security;

import com.ecommerce.common.exception.ApiError;
import com.ecommerce.common.web.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Responde falhas de autenticacao (401) e autorizacao (403) no mesmo formato JSON
 * {@link ApiError} usado pelo restante da API.
 *
 * <p>Sem isso, o Spring Security responderia com corpo vazio, e o cliente nao saberia o motivo.
 * A mensagem e generica de proposito: nao revelamos se o token expirou, se a assinatura
 * e invalida etc., para nao ajudar um atacante.</p>
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Authentication required: provide a valid Bearer token");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
            throws IOException {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), CorrelationId.current(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
