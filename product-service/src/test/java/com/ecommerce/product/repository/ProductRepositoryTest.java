package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        // O banco H2 em memoria e compartilhado entre os contextos de teste em cache;
        // limpamos para nao depender da ordem de execucao. deleteAllInBatch executa o DELETE imediatamente;
        // deleteAll() so removeria no flush, e o Hibernate executa INSERTs antes de DELETEs no flush.
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        electronics = categoryRepository.save(new Category("Eletronicos", null));
        books = categoryRepository.save(new Category("Livros", null));
        productRepository.save(product("MOUSE-1", "Mouse Gamer", electronics));
        productRepository.save(product("KEYB-1", "Teclado", electronics));
        Product inactive = product("BOOK-1", "Livro de Java", books);
        inactive.deactivate();
        productRepository.save(inactive);
    }

    private static Product product(String sku, String name, Category category) {
        return Product.builder().sku(sku).name(name).price(new BigDecimal("10.00")).category(category).build();
    }

    @Test
    void shouldFilterByCategoryActiveAndName() {
        var spec = ProductSpecifications.hasCategory(electronics.getId())
                .and(ProductSpecifications.isActive(true))
                .and(ProductSpecifications.nameContains("MOUSE"));

        var page = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("MOUSE-1");
        assertThat(page.getContent().getFirst().getCategory().getName()).isEqualTo("Eletronicos");
    }

    @Test
    void shouldFindInactiveProducts() {
        var page = productRepository.findAll(ProductSpecifications.isActive(false), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("BOOK-1");
    }

    @Test
    void shouldAnswerExistenceQueries() {
        assertThat(productRepository.existsBySku("KEYB-1")).isTrue();
        assertThat(productRepository.existsBySku("NOPE")).isFalse();
        assertThat(productRepository.existsByCategoryId(books.getId())).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("livros")).isTrue();
    }
}
