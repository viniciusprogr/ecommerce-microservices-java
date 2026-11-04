package com.ecommerce.order.config;

import com.ecommerce.common.security.Roles;
import com.ecommerce.common.security.StatelessJwtSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Criar e consultar pedidos: USER ou ADMIN. Alterar status: apenas ADMIN (ADR-0005). */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StatelessJwtSecurity jwtSecurity) throws Exception {
        jwtSecurity.apply(http).authorizeHttpRequests(auth -> auth
                .requestMatchers(StatelessJwtSecurity.PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole(Roles.ADMIN)
                .requestMatchers("/api/orders/**").hasAnyRole(Roles.USER, Roles.ADMIN)
                .anyRequest().denyAll());
        return http.build();
    }
}
