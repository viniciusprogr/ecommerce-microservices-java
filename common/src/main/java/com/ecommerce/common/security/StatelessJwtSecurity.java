package com.ecommerce.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Aplica a configuracao de seguranca comum a todos os servicos (resource servers).
 *
 * <ul>
 *   <li>Sem sessao HTTP (STATELESS): cada requisicao traz seu proprio token.</li>
 *   <li>CSRF desabilitado: nao usamos cookies de sessao, entao o ataque CSRF nao se aplica.</li>
 *   <li>Validacao de JWT com o conversor de roles.</li>
 *   <li>Erros 401/403 em JSON.</li>
 * </ul>
 *
 * <p>As regras de autorizacao (quem pode acessar o que) ficam em cada servico,
 * pois dependem do dominio. Exemplo de uso:</p>
 *
 * <pre>{@code
 * jwtSecurity.apply(http)
 *     .authorizeHttpRequests(auth -> auth
 *         .requestMatchers(StatelessJwtSecurity.PUBLIC_PATHS).permitAll()
 *         .anyRequest().authenticated());
 * return http.build();
 * }</pre>
 */
public class StatelessJwtSecurity {

    /** Endpoints publicos: health checks e documentacao OpenAPI. */
    public static final String[] PUBLIC_PATHS = {
        "/actuator/health/**",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/error"
    };

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JsonSecurityErrorHandler errorHandler;

    public StatelessJwtSecurity(JwtAuthenticationConverter jwtAuthenticationConverter,
                                JsonSecurityErrorHandler errorHandler) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.errorHandler = errorHandler;
    }

    public HttpSecurity apply(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
    }
}
