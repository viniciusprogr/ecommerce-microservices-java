package com.ecommerce.inventory.config;

import com.ecommerce.common.security.Roles;
import com.ecommerce.common.security.StatelessJwtSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autorizacao do Inventory Service. As reservas aceitam qualquer usuario autenticado porque o
 * Order Service repassa o token do cliente; a protecao contra acesso externo e feita no gateway.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StatelessJwtSecurity jwtSecurity) throws Exception {
        jwtSecurity.apply(http).authorizeHttpRequests(auth -> auth
                .requestMatchers(StatelessJwtSecurity.PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/inventory/reservations/**").hasAnyRole(Roles.USER, Roles.ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole(Roles.USER, Roles.ADMIN)
                .requestMatchers("/api/inventory/**").hasRole(Roles.ADMIN)
                .anyRequest().denyAll());
        return http.build();
    }
}
