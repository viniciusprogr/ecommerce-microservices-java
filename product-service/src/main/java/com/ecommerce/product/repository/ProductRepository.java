package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repositorio de produtos.
 *
 * <p>{@link JpaSpecificationExecutor} permite montar filtros dinamicos (ver {@link ProductSpecifications}).
 * {@code @EntityGraph} carrega a categoria na mesma consulta (JOIN), evitando o problema "N+1":
 * uma consulta para a lista e mais uma para cada categoria.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    @Override
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);
}
