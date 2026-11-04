package com.ecommerce.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Configuracao de seguranca do gateway, toda vinda de variaveis de ambiente.
 *
 * @param jwt   emissao e validacao de tokens
 * @param users usuarios aceitos no login (substituto didatico de um Identity Provider, ver ADR-0005)
 */
@Validated
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotEmpty List<UserAccount> users) {

    /**
     * @param secret     chave HMAC compartilhada com os servicos (minimo 32 caracteres)
     * @param issuer     valor do claim {@code iss}
     * @param expiration validade do token
     */
    public record Jwt(
            @NotBlank @Size(min = 32, message = "must have at least 32 characters (256 bits) for HS256") String secret,
            @NotBlank String issuer,
            @NotNull Duration expiration) {
    }

    /**
     * @param username login
     * @param password senha em texto puro vinda do ambiente; e convertida para BCrypt na inicializacao
     * @param roles    papeis, sem o prefixo ROLE_
     */
    public record UserAccount(@NotBlank String username, @NotBlank String password, @NotEmpty List<String> roles) {
    }
}
