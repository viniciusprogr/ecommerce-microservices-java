package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Produto do catalogo.
 *
 * <p>Dinheiro e sempre {@link BigDecimal}: {@code double} tem erros de arredondamento binario
 * ({@code 0.1 + 0.2 != 0.3}). "Disponibilidade" aqui e comercial ({@code active}): o produto pode ser vendido.
 * A disponibilidade fisica (quantidade) e responsabilidade do Inventory Service.</p>
 *
 * <p>Usa o padrao Builder porque tem varios atributos, alguns opcionais, o que deixaria um construtor confuso.</p>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /** LAZY: a categoria so e carregada do banco quando acessada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Product() {
    }

    private Product(Builder builder) {
        this.sku = builder.sku;
        this.name = builder.name;
        this.description = builder.description;
        this.price = builder.price;
        this.category = builder.category;
        this.active = true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void update(String newName, String newDescription, BigDecimal newPrice, Category newCategory) {
        this.name = newName;
        this.description = newDescription;
        this.price = newPrice;
        this.category = newCategory;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
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

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
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
        if (!(other instanceof Product that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Builder fluente: {@code Product.builder().sku("A1").name("Mouse").price(p).category(c).build()}. */
    public static final class Builder {
        private String sku;
        private String name;
        private String description;
        private BigDecimal price;
        private Category category;

        private Builder() {
        }

        public Builder sku(String value) {
            this.sku = value;
            return this;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder price(BigDecimal value) {
            this.price = value;
            return this;
        }

        public Builder category(Category value) {
            this.category = value;
            return this;
        }

        public Product build() {
            Objects.requireNonNull(sku, "sku is required");
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(price, "price is required");
            Objects.requireNonNull(category, "category is required");
            return new Product(this);
        }
    }
}
