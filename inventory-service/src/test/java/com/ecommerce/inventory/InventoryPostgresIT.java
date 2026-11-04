package com.ecommerce.inventory;

import com.ecommerce.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationItem;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationRequest;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import com.ecommerce.inventory.service.InventoryService;
import com.ecommerce.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida no PostgreSQL real o bloqueio pessimista: 20 reservas concorrentes de 1 unidade
 * disputando um estoque de 5. Exatamente 5 devem ser aceitas. Requer Docker.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void shouldNeverOversellUnderConcurrency() throws Exception {
        inventoryService.create(new CreateInventoryItemRequest(1L, 5));
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String reference = "order-" + i;
            attempts.add(() -> {
                try {
                    reservationService.reserve(new ReservationRequest(reference, List.of(new ReservationItem(1L, 1))));
                    return true;
                } catch (InsufficientStockException ex) {
                    return false;
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        long accepted;
        try {
            accepted = executor.invokeAll(attempts).stream().filter(this::succeeded).count();
        } finally {
            executor.shutdown();
        }

        assertThat(accepted).isEqualTo(5);
        var item = inventoryService.findByProductId(1L);
        assertThat(item.quantityReserved()).isEqualTo(5);
        assertThat(item.available()).isZero();
    }

    private boolean succeeded(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
