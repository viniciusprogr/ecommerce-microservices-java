package com.ecommerce.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope estavel para respostas paginadas.
 *
 * <p>Serializar {@code PageImpl} diretamente expoe detalhes internos do Spring Data e
 * pode mudar entre versoes. Este record define um contrato JSON explicito.</p>
 *
 * @param content       itens da pagina
 * @param page          numero da pagina (comeca em 0)
 * @param size          tamanho da pagina
 * @param totalElements total de itens
 * @param totalPages    total de paginas
 * @param <T>           tipo dos itens
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
