package com.ecommerce.inventory.entity;

import com.ecommerce.inventory.exception.InvalidStockOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes de dominio puro: sem Spring, sem banco, sem mocks. Os mais rapidos e baratos da piramide. */
class InventoryItemTest {

    @Test
    void shouldReserveAndReduceAvailable() {
        InventoryItem item = new InventoryItem(1L, 10);

        item.reserve(3);

        assertThat(item.getQuantityOnHand()).isEqualTo(10);
        assertThat(item.getQuantityReserved()).isEqualTo(3);
        assertThat(item.available()).isEqualTo(7);
    }

    @Test
    void shouldNotReserveMoreThanAvailable() {
        InventoryItem item = new InventoryItem(1L, 5);
        item.reserve(4);

        assertThat(item.canReserve(2)).isFalse();
        assertThatThrownBy(() -> item.reserve(2))
                .isInstanceOf(InvalidStockOperationException.class)
                .hasMessageContaining("only 1 available");
    }

    @Test
    void shouldCommitReservedQuantity() {
        InventoryItem item = new InventoryItem(1L, 10);
        item.reserve(4);

        item.commit(4);

        assertThat(item.getQuantityOnHand()).isEqualTo(6);
        assertThat(item.getQuantityReserved()).isZero();
        assertThat(item.available()).isEqualTo(6);
    }

    @Test
    void shouldReleaseReservedQuantity() {
        InventoryItem item = new InventoryItem(1L, 10);
        item.reserve(4);

        item.release(4);

        assertThat(item.available()).isEqualTo(10);
    }

    @Test
    void shouldNotReleaseOrCommitMoreThanReserved() {
        InventoryItem item = new InventoryItem(1L, 10);
        item.reserve(1);

        assertThatThrownBy(() -> item.release(2)).isInstanceOf(InvalidStockOperationException.class);
        assertThatThrownBy(() -> item.commit(2)).isInstanceOf(InvalidStockOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveQuantities(int quantity) {
        InventoryItem item = new InventoryItem(1L, 10);

        assertThat(item.canReserve(quantity)).isFalse();
        assertThatThrownBy(() -> item.reserve(quantity)).isInstanceOf(InvalidStockOperationException.class);
    }

    @Test
    void shouldNotAdjustBelowReserved() {
        InventoryItem item = new InventoryItem(1L, 10);
        item.reserve(6);

        assertThatThrownBy(() -> item.adjustOnHand(5)).isInstanceOf(InvalidStockOperationException.class);
        item.adjustOnHand(6);
        assertThat(item.available()).isZero();
    }

    @Test
    void shouldRejectNegativeInitialQuantity() {
        assertThatThrownBy(() -> new InventoryItem(1L, -1)).isInstanceOf(InvalidStockOperationException.class);
    }
}
