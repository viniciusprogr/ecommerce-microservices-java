package com.ecommerce.order.mapper;

import com.ecommerce.order.dto.OrderDtos.OrderItemResponse;
import com.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import org.springframework.stereotype.Component;

/** Conversao de pedidos para DTOs de saida. */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toResponse).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
