package com.ecommerce.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Dados de entrada para criar ou atualizar um cliente.
 *
 * <p>As anotacoes do Bean Validation sao verificadas pelo Spring quando o controller usa {@code @Valid}.
 * Se alguma falhar, a requisicao nem chega ao service: o cliente recebe 400 com a lista de campos invalidos.</p>
 */
@Schema(description = "Dados do cliente")
public record CustomerRequest(
        @Schema(example = "Maria")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(example = "Silva")
        @NotBlank @Size(max = 100) String lastName,

        @Schema(example = "maria.silva@email.com")
        @NotBlank @Email @Size(max = 150) String email,

        @Schema(description = "CPF com ou sem pontuacao", example = "529.982.247-25")
        @NotBlank @CPF String document,

        @Schema(example = "+55 11 98765-4321")
        @Pattern(regexp = "^[0-9+()\\- ]{8,20}$", message = "must contain 8 to 20 digits or phone symbols")
        String phone) {
}
