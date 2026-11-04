package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.ecommerce.inventory.entity.InventoryItem;
import com.ecommerce.inventory.exception.DuplicateInventoryItemException;
import com.ecommerce.inventory.exception.InventoryItemNotFoundException;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository repository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(repository, new InventoryMapper());
    }

    @Test
    void shouldCreateInventoryItem() {
        when(repository.existsByProductId(1L)).thenReturn(false);
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(new CreateInventoryItemRequest(1L, 50));

        assertThat(response.available()).isEqualTo(50);
    }

    @Test
    void shouldRejectDuplicatedProduct() {
        when(repository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateInventoryItemRequest(1L, 5)))
                .isInstanceOf(DuplicateInventoryItemException.class);
    }

    @Test
    void shouldCheckAvailability() {
        when(repository.findByProductId(1L)).thenReturn(Optional.of(new InventoryItem(1L, 3)));

        assertThat(service.checkAvailability(1L, 3).sufficient()).isTrue();
        assertThat(service.checkAvailability(1L, 4).sufficient()).isFalse();
    }

    @Test
    void shouldAdjustQuantity() {
        InventoryItem item = new InventoryItem(1L, 3);
        when(repository.findByProductId(1L)).thenReturn(Optional.of(item));
        when(repository.saveAndFlush(item)).thenReturn(item);

        assertThat(service.adjustOnHand(1L, 30).quantityOnHand()).isEqualTo(30);
    }

    @Test
    void shouldListAndThrowNotFound() {
        var pageable = PageRequest.of(0, 5);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(new InventoryItem(1L, 1)), pageable, 1));
        when(repository.findByProductId(2L)).thenReturn(Optional.empty());

        assertThat(service.findAll(pageable).totalElements()).isEqualTo(1);
        assertThatThrownBy(() -> service.findByProductId(2L)).isInstanceOf(InventoryItemNotFoundException.class);
    }
}
