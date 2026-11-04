package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio de reservas; carrega as linhas junto (EntityGraph) para evitar consultas extras. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Reservation> findByReference(String reference);
}
