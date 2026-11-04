package com.ecommerce.customer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do Swagger/OpenAPI. O esquema "bearerAuth" adiciona o botao "Authorize" no Swagger UI,
 * onde se cola o token obtido em {@code POST /auth/login} no gateway.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Customer Service API", version = "1.0.0", description = "Cadastro e gerenciamento de clientes"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
