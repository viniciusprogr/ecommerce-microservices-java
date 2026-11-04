package com.ecommerce.gateway.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTOs do endpoint de login. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 200) String password) {
    }

    /**
     * @param accessToken JWT assinado
     * @param tokenType   sempre "Bearer"
     * @param expiresIn   validade em segundos
     * @param roles       papeis do usuario
     */
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, List<String> roles) {
    }
}
