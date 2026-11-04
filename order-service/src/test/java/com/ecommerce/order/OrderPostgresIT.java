package com.ecommerce.order;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida migrations, cascade dos itens e filtros no PostgreSQL real. Requer Docker. */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void shouldPersistOrderWithItemsAndFilterOnPostgres() {
        Order order = new Order(UUID.randomUUID().toString(), 7L, List.of(
                new OrderItem(1L, "Mouse", new BigDecimal("59.90"), 2),
                new OrderItem(2L, "Teclado", new BigDecimal("120.00"), 1)));
        Order saved = repository.saveAndFlush(order);

        var found = repository.findWithItemsById(saved.getId()).orElseThrow();
        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getTotalAmount()).isEqualByComparingTo("239.80");

        var page = repository.findAll(OrderRepository.hasCustomer(7L).and(OrderRepository.hasStatus(OrderStatus.CONFIRMED)),
                PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
