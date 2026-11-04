package com.ecommerce.inventory.entity;

import com.ecommerce.inventory.exception.InvalidReservationStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reserva de estoque feita por um pedido. Agregado com suas {@link ReservationLine linhas}.
 *
 * <p>A {@code reference} (o numero do pedido) e unica: e ela que torna a operacao idempotente.
 * Se o Order Service repetir a mesma chamada, encontramos a reserva existente e nao reservamos duas vezes.</p>
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /** cascade ALL + orphanRemoval: as linhas sao salvas e removidas junto com a reserva. */
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
    }

    public Reservation(String reference) {
        this.reference = reference;
        this.status = ReservationStatus.RESERVED;
    }

    public void addLine(Long productId, int quantity) {
        lines.add(new ReservationLine(this, productId, quantity));
    }

    /**
     * @return {@code true} se a reserva mudou de estado; {@code false} se ja estava confirmada (idempotente)
     */
    public boolean markCommitted() {
        return transitionTo(ReservationStatus.COMMITTED);
    }

    /**
     * @return {@code true} se a reserva mudou de estado; {@code false} se ja estava liberada (idempotente)
     */
    public boolean markReleased() {
        return transitionTo(ReservationStatus.RELEASED);
    }

    private boolean transitionTo(ReservationStatus target) {
        if (status == target) {
            return false;
        }
        if (status != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(reference, status, target);
        }
        status = target;
        return true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public List<ReservationLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Reservation that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
