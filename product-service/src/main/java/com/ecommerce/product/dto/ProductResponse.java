package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Representacao de saida de um produto, ja com os dados basicos da categoria. */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
