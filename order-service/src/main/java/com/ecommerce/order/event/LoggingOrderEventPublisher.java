package com.ecommerce.order.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementacao inicial: registra o evento em log estruturado.
 * Util para estudo e como "auditoria" ate existir um broker de mensagens.
 */
@Component
public class LoggingOrderEventPublisher implements OrderEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingOrderEventPublisher.class);

    @Override
    public void publish(OrderEvent event) {
        LOG.info("OrderEvent type={} orderId={} orderNumber={} customerId={} status={} total={}",
                event.type(), event.orderId(), event.orderNumber(), event.customerId(), event.status(),
                event.totalAmount());
    }
}
