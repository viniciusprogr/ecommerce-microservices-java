package com.ecommerce.customer.config;

import com.ecommerce.common.security.Roles;
import com.ecommerce.common.security.StatelessJwtSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de autorizacao do Customer Service (ver matriz na ADR-0005).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StatelessJwtSecurity jwtSecurity) throws Exception {
        jwtSecurity.apply(http).authorizeHttpRequests(auth -> auth
                .requestMatchers(StatelessJwtSecurity.PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole(Roles.ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/customers/*/activate").hasRole(Roles.ADMIN)
                .requestMatchers("/api/customers/**").hasAnyRole(Roles.USER, Roles.ADMIN)
                .anyRequest().denyAll());
        return http.build();
    }
}
