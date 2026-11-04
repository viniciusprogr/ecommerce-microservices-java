package com.ecommerce.product;

import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.CategoryService;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida migrations, especificacoes (LIKE/lower) e NUMERIC no PostgreSQL real. Requer Docker. */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ProductPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldCreateAndSearchProductsOnPostgres() {
        var category = categoryService.create(new CategoryRequest("Eletronicos", null));
        productService.create(new ProductRequest("MOUSE-001", "Mouse Gamer", null, new BigDecimal("149.99"), category.id()));

        var page = productService.findAll(category.id(), true, "gamer", PageRequest.of(0, 10));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().price()).isEqualByComparingTo("149.99");
    }
}
