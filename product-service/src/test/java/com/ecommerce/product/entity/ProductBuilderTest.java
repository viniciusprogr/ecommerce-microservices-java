package com.ecommerce.product.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductBuilderTest {

    @Test
    void shouldBuildActiveProduct() {
        Product product = Product.builder().sku("A-1").name("Item").description("d")
                .price(BigDecimal.TEN).category(new Category("C", null)).build();

        assertThat(product.isActive()).isTrue();
        assertThat(product.getSku()).isEqualTo("A-1");
        assertThat(product.getDescription()).isEqualTo("d");
    }

    @Test
    void shouldRequireMandatoryFields() {
        assertThatThrownBy(() -> Product.builder().sku("A-1").name("Item").price(BigDecimal.TEN).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("category is required");
        assertThatThrownBy(() -> Product.builder().build()).hasMessage("sku is required");
    }
}
