package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtros reutilizaveis de produto (padrao Specification).
 *
 * <p>Cada metodo devolve um criterio; o service combina apenas os que foram informados.
 * Isso evita consultas JPQL com {@code :param is null or ...}, que no PostgreSQL causam erros de
 * tipo de parametro e impedem o uso de indices.</p>
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> nameContains(String text) {
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }
}
