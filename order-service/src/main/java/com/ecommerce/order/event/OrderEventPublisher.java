package com.ecommerce.order.event;

/**
 * Porta de saida para publicacao de eventos de pedido.
 *
 * <p>O {@code OrderService} depende desta INTERFACE, nao de RabbitMQ, Kafka ou log (Dependency Inversion).
 * Para migrar para mensageria, basta criar outra implementacao (ex.: {@code RabbitOrderEventPublisher})
 * sem tocar nas regras de negocio. Ver docs/architecture/communication.md.</p>
 */
public interface OrderEventPublisher {

    void publish(OrderEvent event);
}
