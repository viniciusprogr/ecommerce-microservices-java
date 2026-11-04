package com.ecommerce.inventory.entity;

import com.ecommerce.inventory.exception.InvalidStockOperationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

/**
 * Posicao de estoque de um produto.
 *
 * <pre>
 * disponivel = quantityOnHand - quantityReserved
 * </pre>
 *
 * <p>As invariantes (nunca reservar mais do que o disponivel, nunca ter estoque negativo) sao garantidas
 * AQUI, na entidade. Assim nenhum service consegue deixar o estoque inconsistente por engano.
 * O banco reforca as mesmas regras com constraints CHECK.</p>
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id do produto no Product Service. Nao ha chave estrangeira: cada servico tem seu banco (ADR-0002). */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected InventoryItem() {
    }

    public InventoryItem(Long productId, int initialQuantity) {
        requireNonNegative(initialQuantity);
        this.productId = productId;
        this.quantityOnHand = initialQuantity;
        this.quantityReserved = 0;
    }

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && available() >= quantity;
    }

    /** Separa a quantidade para um pedido: ela continua no estoque fisico, mas nao pode ser vendida de novo. */
    public void reserve(int quantity) {
        requirePositive(quantity);
        if (!canReserve(quantity)) {
            throw new InvalidStockOperationException("Cannot reserve " + quantity + " units of product " + productId
                    + ": only " + available() + " available");
        }
        quantityReserved += quantity;
    }

    /** Devolve uma quantidade reservada para o disponivel (pedido cancelado). */
    public void release(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InvalidStockOperationException("Cannot release more than reserved for product " + productId);
        }
        quantityReserved -= quantity;
    }

    /** Baixa definitiva: a quantidade reservada sai fisicamente do estoque (pedido enviado). */
    public void commit(int quantity) {
        requirePositive(quantity);
        if (quantity > quantityReserved) {
            throw new InvalidStockOperationException("Cannot commit more than reserved for product " + productId);
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
    }

    /** Ajuste manual (inventario fisico). Nao pode ficar abaixo do que ja esta reservado. */
    public void adjustOnHand(int newQuantity) {
        requireNonNegative(newQuantity);
        if (newQuantity < quantityReserved) {
            throw new InvalidStockOperationException("Quantity on hand (" + newQuantity
                    + ") cannot be lower than the reserved quantity (" + quantityReserved + ")");
        }
        quantityOnHand = newQuantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockOperationException("Quantity must be positive");
        }
    }

    private static void requireNonNegative(int quantity) {
        if (quantity < 0) {
            throw new InvalidStockOperationException("Quantity cannot be negative");
        }
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

    public Long getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InventoryItem that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
