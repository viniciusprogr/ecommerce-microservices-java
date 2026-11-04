package com.ecommerce.customer.dto;

import java.time.Instant;

/**
 * Representacao de saida do cliente. Nunca expomos a entidade JPA diretamente:
 * assim o contrato da API nao muda quando o modelo de persistencia muda.
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String document,
        String phone,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
