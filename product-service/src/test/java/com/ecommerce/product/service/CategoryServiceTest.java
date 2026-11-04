package com.ecommerce.product.service;

import com.ecommerce.product.dto.CategoryRequest;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.CatalogConflictException;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, productRepository, new ProductMapper());
    }

    private static Category category(Long id, String name) {
        Category category = new Category(name, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    void shouldCreateCategoryWithTrimmedName() {
        when(categoryRepository.existsByNameIgnoreCase("Livros")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(new CategoryRequest("  Livros ", "desc")).name()).isEqualTo("Livros");
    }

    @Test
    void shouldRejectDuplicatedName() {
        when(categoryRepository.existsByNameIgnoreCase("Livros")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Livros", null)))
                .isInstanceOf(CatalogConflictException.class);
    }

    @Test
    void shouldListSortedByName() {
        when(categoryRepository.findAll(Sort.by("name"))).thenReturn(List.of(category(1L, "A"), category(2L, "B")));

        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    void shouldUpdateCategory() {
        Category existing = category(1L, "Old");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);

        assertThat(service.update(1L, new CategoryRequest("New", "d")).name()).isEqualTo("New");
    }

    @Test
    void shouldRejectRenameToExistingName() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Old")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new CategoryRequest("Taken", null)))
                .isInstanceOf(CatalogConflictException.class);
    }

    @Test
    void shouldNotDeleteCategoryInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "A")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(CatalogConflictException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void shouldDeleteUnusedCategory() {
        Category existing = category(1L, "A");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void shouldThrowNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(CategoryNotFoundException.class);
    }
}
