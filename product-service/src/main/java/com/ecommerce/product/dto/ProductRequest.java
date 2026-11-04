package com.ecommerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Dados de entrada para criar ou atualizar um produto. */
public record ProductRequest(
        @Schema(description = "Codigo unico do produto (letras maiusculas, numeros e hifen)", example = "MOUSE-001")
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z0-9-]+$", message = "must contain only A-Z, 0-9 and '-'")
        String sku,

        @Schema(example = "Mouse sem fio")
        @NotBlank @Size(max = 150) String name,

        @Schema(example = "Mouse optico 1600 DPI")
        @Size(max = 1000) String description,

        @Schema(example = "99.90")
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal price,

        @Schema(example = "1")
        @NotNull @Positive Long categoryId) {
}
