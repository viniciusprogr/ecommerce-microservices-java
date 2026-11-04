package com.ecommerce.gateway.auth;

import com.ecommerce.gateway.auth.AuthDtos.LoginRequest;
import com.ecommerce.gateway.auth.AuthDtos.TokenResponse;
import com.ecommerce.gateway.config.GatewaySecurityProperties;
import com.ecommerce.gateway.config.SecurityConfig;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Login: troca usuario e senha por um JWT.
 *
 * <p>Codigo reativo: o metodo devolve um {@link Mono} (0 ou 1 valor no futuro). Nada bloqueia a thread enquanto
 * a senha e verificada; o WebFlux assina o Mono e escreve a resposta quando ele completar.</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final ReactiveAuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(GatewaySecurityProperties properties, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.authenticationManager = SecurityConfig.loginAuthenticationManager(properties, passwordEncoder);
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        return authenticationManager.authenticate(credentials)
                .map(tokenService::issue)
                .doOnNext(token -> LOG.info("Token issued for user={} roles={}", request.username(), token.roles()));
    }
}
