package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.CatalogConflictException;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    private ProductService service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryService, new ProductMapper());
        category = new Category("Eletronicos", null);
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    private Product product(Long id) {
        Product product = Product.builder().sku("MOUSE-001").name("Mouse").price(new BigDecimal("99.90"))
                .category(category).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static ProductRequest request(String sku, String price) {
        return new ProductRequest(sku, "Mouse", "Optico", new BigDecimal(price), 1L);
    }

    @Test
    void shouldCreateProduct() {
        when(productRepository.existsBySku("MOUSE-001")).thenReturn(false);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        ProductResponse response = service.create(request("MOUSE-001", "99.90"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.categoryName()).isEqualTo("Eletronicos");
        assertThat(response.price()).isEqualByComparingTo("99.90");
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldRejectDuplicatedSku() {
        when(productRepository.existsBySku("MOUSE-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("MOUSE-001", "10.00")))
                .isInstanceOf(CatalogConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldPropagateCategoryNotFound() {
        when(productRepository.existsBySku("MOUSE-001")).thenReturn(false);
        when(categoryService.getCategory(1L)).thenThrow(new CategoryNotFoundException(1L));

        assertThatThrownBy(() -> service.create(request("MOUSE-001", "10.00")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldThrowNotFound_whenProductIsMissing() {
        when(productRepository.findWithCategoryById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldUpdatePriceAndName() {
        Product existing = product(5L);
        when(productRepository.findWithCategoryById(5L)).thenReturn(Optional.of(existing));
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductResponse response = service.update(5L, request("MOUSE-001", "79.90"));

        assertThat(response.price()).isEqualByComparingTo("79.90");
    }

    @Test
    void shouldNotAllowSkuChange() {
        when(productRepository.findWithCategoryById(5L)).thenReturn(Optional.of(product(5L)));

        assertThatThrownBy(() -> service.update(5L, request("OTHER-SKU", "10.00")))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void shouldDeactivateAndActivate() {
        Product existing = product(5L);
        when(productRepository.findWithCategoryById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        service.deactivate(5L);
        assertThat(existing.isActive()).isFalse();

        assertThat(service.activate(5L).active()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSearchWithFilters() {
        var pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product(1L)), pageable, 1));

        var page = service.findAll(1L, true, "mou", pageable);

        assertThat(page.content()).extracting(ProductResponse::sku).containsExactly("MOUSE-001");
    }
}
