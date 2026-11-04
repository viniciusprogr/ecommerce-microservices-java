package com.ecommerce.common.security;

/**
 * Papeis (roles) reconhecidos pelos servicos.
 *
 * <p>O token JWT carrega o claim {@value #CLAIM} com valores sem prefixo (ex.: {@code ["ADMIN"]}).
 * O Spring Security converte para authorities {@code ROLE_ADMIN}, permitindo usar
 * {@code hasRole("ADMIN")} nas regras de autorizacao.</p>
 */
public final class Roles {

    public static final String CLAIM = "roles";
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private Roles() {
    }
}
