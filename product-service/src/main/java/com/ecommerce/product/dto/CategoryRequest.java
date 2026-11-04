package com.ecommerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de entrada de uma categoria. */
public record CategoryRequest(
        @Schema(example = "Eletronicos") @NotBlank @Size(max = 80) String name,
        @Schema(example = "Aparelhos eletronicos e acessorios") @Size(max = 255) String description) {
}
