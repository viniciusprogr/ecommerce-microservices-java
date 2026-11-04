package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.BusinessRuleException;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationItem;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationRequest;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.ecommerce.inventory.entity.InventoryItem;
import com.ecommerce.inventory.entity.Reservation;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.InvalidReservationStateException;
import com.ecommerce.inventory.exception.ReservationNotFoundException;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private InventoryItemRepository itemRepository;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, itemRepository, new InventoryMapper());
    }

    private static ReservationRequest request(ReservationItem... items) {
        return new ReservationRequest("order-1", List.of(items));
    }

    @Test
    void shouldReserveAllItems() {
        InventoryItem mouse = new InventoryItem(1L, 10);
        InventoryItem keyboard = new InventoryItem(2L, 5);
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.empty());
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(mouse, keyboard));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = service.reserve(request(new ReservationItem(1L, 2), new ReservationItem(2L, 5)));

        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.items()).hasSize(2);
        assertThat(mouse.available()).isEqualTo(8);
        assertThat(keyboard.available()).isZero();
    }

    @Test
    void shouldReserveNothing_whenAnyItemIsShort() {
        InventoryItem mouse = new InventoryItem(1L, 10);
        InventoryItem keyboard = new InventoryItem(2L, 1);
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.empty());
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(mouse, keyboard));

        assertThatThrownBy(() -> service.reserve(request(new ReservationItem(1L, 2), new ReservationItem(2L, 3))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("product 2 requested 3, available 1");
        assertThat(mouse.available()).as("all or nothing").isEqualTo(10);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldFail_whenProductHasNoInventory() {
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.empty());
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.reserve(request(new ReservationItem(9L, 1))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("product 9 has no inventory");
    }

    @Test
    void shouldBeIdempotent_whenReferenceAlreadyExists() {
        Reservation existing = new Reservation("order-1");
        existing.addLine(1L, 2);
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.of(existing));

        ReservationResponse response = service.reserve(request(new ReservationItem(1L, 2)));

        assertThat(response.reference()).isEqualTo("order-1");
        verify(itemRepository, never()).findAllForUpdate(anyCollection());
    }

    @Test
    void shouldRejectDuplicatedProducts() {
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(request(new ReservationItem(1L, 1), new ReservationItem(1L, 2))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldCommitReservation() {
        InventoryItem mouse = new InventoryItem(1L, 10);
        mouse.reserve(2);
        Reservation reservation = new Reservation("order-1");
        reservation.addLine(1L, 2);
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.of(reservation));
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(mouse));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);

        assertThat(service.commit("order-1").status()).isEqualTo("COMMITTED");
        assertThat(mouse.getQuantityOnHand()).isEqualTo(8);
        assertThat(mouse.getQuantityReserved()).isZero();
    }

    @Test
    void shouldReleaseReservationAndBeIdempotent() {
        InventoryItem mouse = new InventoryItem(1L, 10);
        mouse.reserve(2);
        Reservation reservation = new Reservation("order-1");
        reservation.addLine(1L, 2);
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.of(reservation));
        when(itemRepository.findAllForUpdate(anyCollection())).thenReturn(List.of(mouse));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);

        service.release("order-1");
        service.release("order-1");

        assertThat(mouse.available()).isEqualTo(10);
        assertThat(reservation.getStatus().name()).isEqualTo("RELEASED");
    }

    @Test
    void shouldRejectReleaseOfCommittedReservation() {
        Reservation reservation = new Reservation("order-1");
        reservation.markCommitted();
        when(reservationRepository.findByReference("order-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.release("order-1")).isInstanceOf(InvalidReservationStateException.class);
    }

    @Test
    void shouldThrowNotFound_whenReservationIsMissing() {
        when(reservationRepository.findByReference("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByReference("nope")).isInstanceOf(ReservationNotFoundException.class);
    }
}
