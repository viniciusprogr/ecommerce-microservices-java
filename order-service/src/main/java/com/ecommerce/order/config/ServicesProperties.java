package com.ecommerce.order.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Enderecos e timeouts dos servicos chamados pelo Order Service.
 *
 * <p>No Docker Compose, os enderecos usam o nome do container (ex.: {@code http://customer-service:8081}),
 * resolvido pelo DNS interno da rede Docker.</p>
 *
 * @param customerUrl    URL base do Customer Service
 * @param productUrl     URL base do Product Service
 * @param inventoryUrl   URL base do Inventory Service
 * @param connectTimeout tempo maximo para abrir a conexao
 * @param readTimeout    tempo maximo para aguardar a resposta
 */
@Validated
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(
        @NotBlank String customerUrl,
        @NotBlank String productUrl,
        @NotBlank String inventoryUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
