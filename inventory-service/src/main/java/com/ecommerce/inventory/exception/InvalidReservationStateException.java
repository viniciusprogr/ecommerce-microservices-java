package com.ecommerce.inventory.exception;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.inventory.entity.ReservationStatus;

/** Transicao invalida, ex.: liberar uma reserva ja confirmada. HTTP 409. */
public class InvalidReservationStateException extends ConflictException {

    public InvalidReservationStateException(String reference, ReservationStatus current, ReservationStatus target) {
        super("Reservation " + reference + " is " + current + " and cannot become " + target);
    }
}
