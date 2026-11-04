package com.ecommerce.order.entity;

import com.ecommerce.order.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static Order newOrder() {
        return new Order("n-1", 1L, List.of(
                new OrderItem(1L, "Mouse", new BigDecimal("99.90"), 2),
                new OrderItem(2L, "Teclado", new BigDecimal("150"), 1)));
    }

    @Test
    void shouldCalculateSubtotalsAndTotal() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getItems()).extracting(OrderItem::getSubtotal)
                .containsExactly(new BigDecimal("199.80"), new BigDecimal("150.00"));
        assertThat(order.getTotalAmount()).isEqualByComparingTo("349.80");
    }

    @Test
    void shouldRequireItemsAndPositiveQuantity() {
        assertThatThrownBy(() -> new Order("n", 1L, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(1L, "x", BigDecimal.ONE, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "CONFIRMED, SHIPPED, true",
        "CONFIRMED, CANCELLED, true",
        "CONFIRMED, DELIVERED, false",
        "SHIPPED, DELIVERED, true",
        "SHIPPED, CANCELLED, false",
        "DELIVERED, CANCELLED, false",
        "CANCELLED, CONFIRMED, false",
        "CONFIRMED, CONFIRMED, false"
    })
    void shouldFollowStateMachine(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void shouldChangeStatusThroughValidPath() {
        Order order = newOrder();

        order.changeStatus(OrderStatus.SHIPPED);
        order.changeStatus(OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldRejectInvalidTransition() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CANCELLED to SHIPPED");
    }
}
