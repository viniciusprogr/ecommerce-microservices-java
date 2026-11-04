package com.ecommerce.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracao de validacao dos tokens JWT emitidos pelo API Gateway.
 *
 * <p>O segredo vem SEMPRE de variavel de ambiente ({@code JWT_SECRET}); nunca do codigo.
 * O tamanho minimo de 32 caracteres atende o requisito de 256 bits do algoritmo HS256.
 * Se a configuracao estiver ausente ou invalida, o servico falha na inicializacao (fail fast).</p>
 *
 * @param secret chave simetrica compartilhada entre gateway e servicos
 * @param issuer emissor esperado no claim {@code iss}
 */
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must have at least 32 characters (256 bits) for HS256") String secret,
        @NotBlank String issuer) {
}
