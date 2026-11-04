package com.ecommerce.order.service;

import com.ecommerce.common.exception.RemoteServiceException;
import com.ecommerce.order.client.CustomerClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.RemoteDtos.RemoteCustomer;
import com.ecommerce.order.client.RemoteDtos.RemoteProduct;
import com.ecommerce.order.client.RemoteDtos.ReservationRequest;
import com.ecommerce.order.dto.OrderDtos.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDtos.OrderItemRequest;
import com.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.InvalidOrderException;
import com.ecommerce.order.exception.InvalidStatusTransitionException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios da orquestracao. Os tres clientes HTTP sao mocks: simulamos respostas e falhas
 * dos outros servicos sem rede, e verificamos que o Order Service reage corretamente (incluindo a compensacao).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private ProductClient productClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(repository, customerClient, productClient, inventoryClient, eventPublisher,
                new OrderMapper(), transactionTemplate);
        // Executa o callback diretamente, sem transacao real.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private static CreateOrderRequest request() {
        return new CreateOrderRequest(1L, List.of(new OrderItemRequest(10L, 2), new OrderItemRequest(20L, 1)));
    }

    private void givenValidCustomerAndProducts() {
        when(customerClient.getCustomer(1L)).thenReturn(new RemoteCustomer(1L, "Maria", "Silva", "m@e.com", true));
        when(productClient.getProduct(10L)).thenReturn(new RemoteProduct(10L, "MOUSE", "Mouse", new BigDecimal("50.00"), true));
        when(productClient.getProduct(20L)).thenReturn(new RemoteProduct(20L, "KEYB", "Teclado", new BigDecimal("100.00"), true));
    }

    private static Order persistedOrder(Long id) {
        Order order = new Order("order-1", 1L, List.of(new OrderItem(10L, "Mouse", new BigDecimal("50.00"), 2)));
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Nested
    class Create {

        @Test
        void shouldCreateOrderWithPriceSnapshotAndReserveStock() {
            givenValidCustomerAndProducts();
            when(repository.save(any(Order.class))).thenAnswer(inv -> {
                Order order = inv.getArgument(0);
                ReflectionTestUtils.setField(order, "id", 99L);
                return order;
            });

            OrderResponse response = service.create(request());

            assertThat(response.id()).isEqualTo(99L);
            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(response.totalAmount()).isEqualByComparingTo("200.00");
            assertThat(response.items()).extracting(i -> i.productName()).containsExactly("Mouse", "Teclado");

            ArgumentCaptor<ReservationRequest> reservation = ArgumentCaptor.forClass(ReservationRequest.class);
            verify(inventoryClient).reserve(reservation.capture());
            assertThat(reservation.getValue().reference()).isEqualTo(response.orderNumber());
            assertThat(reservation.getValue().items()).hasSize(2);

            ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
            verify(eventPublisher).publish(event.capture());
            assertThat(event.getValue().type()).isEqualTo(OrderEvent.Type.ORDER_CREATED);
        }

        @Test
        void shouldRejectInactiveCustomer() {
            when(customerClient.getCustomer(1L)).thenReturn(new RemoteCustomer(1L, "M", "S", "m@e.com", false));

            assertThatThrownBy(() -> service.create(request()))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessage("Customer 1 is inactive");
            verifyNoInteractions(inventoryClient, repository);
        }

        @Test
        void shouldRejectInactiveProduct() {
            when(customerClient.getCustomer(1L)).thenReturn(new RemoteCustomer(1L, "M", "S", "m@e.com", true));
            when(productClient.getProduct(10L)).thenReturn(new RemoteProduct(10L, "X", "X", BigDecimal.ONE, false));

            assertThatThrownBy(() -> service.create(request()))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("not available");
            verifyNoInteractions(inventoryClient, repository);
        }

        @Test
        void shouldRejectDuplicatedProducts_beforeCallingOtherServices() {
            var duplicated = new CreateOrderRequest(1L, List.of(new OrderItemRequest(10L, 1), new OrderItemRequest(10L, 2)));

            assertThatThrownBy(() -> service.create(duplicated)).isInstanceOf(InvalidOrderException.class);
            verifyNoInteractions(customerClient, productClient, inventoryClient, repository);
        }

        @Test
        void shouldNotSaveOrder_whenStockIsInsufficient() {
            givenValidCustomerAndProducts();
            when(inventoryClient.reserve(any())).thenThrow(new InsufficientStockException("Insufficient stock"));

            assertThatThrownBy(() -> service.create(request())).isInstanceOf(InsufficientStockException.class);
            verify(repository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void shouldReleaseReservation_whenSavingFails() {
            givenValidCustomerAndProducts();
            when(repository.save(any(Order.class))).thenThrow(new DataAccessResourceFailureException("db down"));

            assertThatThrownBy(() -> service.create(request())).isInstanceOf(DataAccessResourceFailureException.class);
            verify(inventoryClient).release(anyString());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void shouldKeepOriginalError_whenCompensationAlsoFails() {
            givenValidCustomerAndProducts();
            when(repository.save(any(Order.class))).thenThrow(new DataAccessResourceFailureException("db down"));
            when(inventoryClient.release(anyString())).thenThrow(new RemoteServiceException("inventory down"));

            assertThatThrownBy(() -> service.create(request())).isInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    @Nested
    class StatusChanges {

        @Test
        void shouldCommitStockWhenShipped() {
            Order order = persistedOrder(5L);
            when(repository.findWithItemsById(5L)).thenReturn(Optional.of(order));
            when(repository.save(order)).thenReturn(order);

            OrderResponse response = service.updateStatus(5L, OrderStatus.SHIPPED);

            assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
            verify(inventoryClient).commit("order-1");
            verify(eventPublisher).publish(any(OrderEvent.class));
        }

        @Test
        void shouldReleaseStockWhenCancelled() {
            Order order = persistedOrder(5L);
            when(repository.findWithItemsById(5L)).thenReturn(Optional.of(order));
            when(repository.save(order)).thenReturn(order);

            service.updateStatus(5L, OrderStatus.CANCELLED);

            verify(inventoryClient).release("order-1");
        }

        @Test
        void shouldNotCallInventoryWhenDelivered() {
            Order order = persistedOrder(5L);
            order.changeStatus(OrderStatus.SHIPPED);
            when(repository.findWithItemsById(5L)).thenReturn(Optional.of(order));
            when(repository.save(order)).thenReturn(order);

            service.updateStatus(5L, OrderStatus.DELIVERED);

            verifyNoInteractions(inventoryClient);
        }

        @Test
        void shouldRejectInvalidTransitionWithoutSideEffects() {
            when(repository.findWithItemsById(5L)).thenReturn(Optional.of(persistedOrder(5L)));

            assertThatThrownBy(() -> service.updateStatus(5L, OrderStatus.DELIVERED))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verifyNoInteractions(inventoryClient);
            verify(repository, never()).save(any());
        }

        @Test
        void shouldKeepStatus_whenInventoryFails() {
            Order order = persistedOrder(5L);
            when(repository.findWithItemsById(5L)).thenReturn(Optional.of(order));
            when(inventoryClient.commit("order-1")).thenThrow(new RemoteServiceException("down"));

            assertThatThrownBy(() -> service.updateStatus(5L, OrderStatus.SHIPPED)).isInstanceOf(RemoteServiceException.class);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowNotFound() {
            when(repository.findWithItemsById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(OrderNotFoundException.class);
        }
    }
}
