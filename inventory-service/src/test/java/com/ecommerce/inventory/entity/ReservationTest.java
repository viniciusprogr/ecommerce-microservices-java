package com.ecommerce.inventory.entity;

import com.ecommerce.inventory.exception.InvalidReservationStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldStartAsReservedWithLines() {
        Reservation reservation = new Reservation("ref-1");
        reservation.addLine(1L, 2);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.getLines()).singleElement()
                .satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
    }

    @Test
    void shouldCommitOnceAndBeIdempotent() {
        Reservation reservation = new Reservation("ref-1");

        assertThat(reservation.markCommitted()).isTrue();
        assertThat(reservation.markCommitted()).isFalse();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void shouldNotReleaseCommittedReservation() {
        Reservation reservation = new Reservation("ref-1");
        reservation.markCommitted();

        assertThatThrownBy(reservation::markReleased)
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("Reservation ref-1 is COMMITTED and cannot become RELEASED");
    }

    @Test
    void shouldNotCommitReleasedReservation() {
        Reservation reservation = new Reservation("ref-1");
        assertThat(reservation.markReleased()).isTrue();

        assertThatThrownBy(reservation::markCommitted).isInstanceOf(InvalidReservationStateException.class);
    }
}
