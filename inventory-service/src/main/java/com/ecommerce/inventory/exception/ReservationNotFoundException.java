package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.ResourceNotFoundException;

/** Reserva inexistente. HTTP 404. */
public class ReservationNotFoundException extends ResourceNotFoundException {

    public ReservationNotFoundException(String reference) {
        super("Reservation " + reference + " not found");
    }
}
