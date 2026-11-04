package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationLineResponse;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.ecommerce.inventory.entity.InventoryItem;
import com.ecommerce.inventory.entity.Reservation;
import org.springframework.stereotype.Component;

/** Conversao entre entidades de estoque e DTOs. */
@Component
public class InventoryMapper {

    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getProductId(), item.getQuantityOnHand(), item.getQuantityReserved(),
                item.available(), item.getUpdatedAt());
    }

    public ReservationResponse toResponse(Reservation reservation) {
        var lines = reservation.getLines().stream()
                .map(line -> new ReservationLineResponse(line.getProductId(), line.getQuantity()))
                .toList();
        return new ReservationResponse(reservation.getReference(), reservation.getStatus().name(), lines,
                reservation.getCreatedAt(), reservation.getUpdatedAt());
    }
}
