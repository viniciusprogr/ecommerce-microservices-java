package com.ecommerce.order.service;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.order.client.CustomerClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.RemoteDtos.RemoteCustomer;
import com.ecommerce.order.client.RemoteDtos.RemoteProduct;
import com.ecommerce.order.client.RemoteDtos.ReservationItem;
import com.ecommerce.order.client.RemoteDtos.ReservationRequest;
import com.ecommerce.order.dto.OrderDtos.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDtos.OrderItemRequest;
import com.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.exception.InvalidOrderException;
import com.ecommerce.order.exception.InvalidStatusTransitionException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestra a criacao e o ciclo de vida dos pedidos (Saga orquestrada, ver docs/architecture/order-flow.md).
 *
 * <p>IMPORTANTE: os metodos que chamam outros servicos NAO sao {@code @Transactional}. Manter uma transacao de
 * banco aberta durante chamadas HTTP prenderia conexoes do pool enquanto esperamos a rede. As gravacoes usam
 * transacoes curtas (do proprio repositorio ou do {@link TransactionTemplate}).</p>
 */
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher eventPublisher;
    private final OrderMapper mapper;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository repository, CustomerClient customerClient, ProductClient productClient,
                        InventoryClient inventoryClient, OrderEventPublisher eventPublisher, OrderMapper mapper,
                        TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Cria um pedido: valida cliente e produtos, reserva estoque e grava.
     * Se a gravacao falhar depois da reserva, executa a compensacao (libera a reserva).
     */
    public OrderResponse create(CreateOrderRequest request) {
        ensureNoDuplicatedProducts(request.items());
        validateCustomer(request.customerId());
        List<OrderItem> items = request.items().stream().map(this::toOrderItem).toList();

        Order order = new Order(UUID.randomUUID().toString(), request.customerId(), items);
        inventoryClient.reserve(new ReservationRequest(order.getOrderNumber(), request.items().stream()
                .map(item -> new ReservationItem(item.productId(), item.quantity()))
                .toList()));

        Order saved;
        try {
            saved = repository.save(order);
        } catch (RuntimeException ex) {
            compensateReservation(order.getOrderNumber(), ex);
            throw ex;
        }
        LOG.info("Order created id={} orderNumber={} total={}", saved.getId(), saved.getOrderNumber(), saved.getTotalAmount());
        eventPublisher.publish(OrderEvent.of(OrderEvent.Type.ORDER_CREATED, saved));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return mapper.toResponse(getOrder(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findAll(Long customerId, OrderStatus status, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();
        if (customerId != null) {
            spec = spec.and(OrderRepository.hasCustomer(customerId));
        }
        if (status != null) {
            spec = spec.and(OrderRepository.hasStatus(status));
        }
        return PageResponse.from(repository.findAll(spec, pageable), mapper::toResponse);
    }

    /**
     * Altera o status aplicando os efeitos no estoque: SHIPPED faz a baixa, CANCELLED libera a reserva.
     * A chamada remota acontece ANTES de gravar: se o Inventory falhar, o status nao muda.
     */
    public OrderResponse updateStatus(Long id, OrderStatus target) {
        Order order = getOrder(id);
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(previous, target);
        }
        switch (target) {
            case SHIPPED -> inventoryClient.commit(order.getOrderNumber());
            case CANCELLED -> inventoryClient.release(order.getOrderNumber());
            default -> LOG.debug("No inventory action for status {}", target);
        }
        order.changeStatus(target);
        OrderResponse response = transactionTemplate.execute(tx -> mapper.toResponse(repository.save(order)));
        LOG.info("Order {} status changed {} -> {}", id, previous, target);
        eventPublisher.publish(OrderEvent.of(OrderEvent.Type.ORDER_STATUS_CHANGED, order));
        return response;
    }

    private void validateCustomer(Long customerId) {
        RemoteCustomer customer = customerClient.getCustomer(customerId);
        if (!customer.active()) {
            throw new InvalidOrderException("Customer " + customerId + " is inactive");
        }
    }

    private OrderItem toOrderItem(OrderItemRequest item) {
        RemoteProduct product = productClient.getProduct(item.productId());
        if (!product.active()) {
            throw new InvalidOrderException("Product " + item.productId() + " is not available for sale");
        }
        return new OrderItem(product.id(), product.name(), product.price(), item.quantity());
    }

    private static void ensureNoDuplicatedProducts(List<OrderItemRequest> items) {
        Set<Long> seen = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!seen.add(item.productId())) {
                throw new InvalidOrderException("Product " + item.productId() + " appears more than once; "
                        + "use the quantity field instead");
            }
        }
    }

    private void compensateReservation(String orderNumber, RuntimeException cause) {
        LOG.error("Failed to persist order {}, releasing stock reservation", orderNumber, cause);
        try {
            inventoryClient.release(orderNumber);
        } catch (RuntimeException releaseFailure) {
            // A reserva ficou "orfa". Em producao, um job de reconciliacao ou uma fila de retentativas resolveria.
            LOG.error("Compensation failed: reservation {} must be released manually", orderNumber, releaseFailure);
        }
    }

    private Order getOrder(Long id) {
        return repository.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
