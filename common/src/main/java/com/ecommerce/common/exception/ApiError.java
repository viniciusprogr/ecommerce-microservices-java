package com.ecommerce.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padronizado de erro retornado por todos os servicos.
 *
 * <p>Usar um unico formato facilita a vida de quem consome a API (front-end, outros servicos)
 * e evita vazar detalhes internos como stack traces ou nomes de tabelas.</p>
 *
 * @param timestamp     momento em que o erro ocorreu
 * @param status        codigo HTTP
 * @param error         descricao curta do status HTTP (ex.: "Not Found")
 * @param message       mensagem legivel para humanos
 * @param path          caminho da requisicao
 * @param correlationId identificador para correlacionar logs entre servicos
 * @param violations    erros de validacao campo a campo (apenas para 400)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> violations) {

    /**
     * Erro de validacao em um campo especifico.
     *
     * @param field   nome do campo
     * @param message motivo da falha
     */
    public record FieldViolation(String field, String message) {
    }
}
