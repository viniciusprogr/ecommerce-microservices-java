package com.ecommerce.customer.entity;

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
 * Cliente do e-commerce (entidade JPA e raiz de agregado do contexto de clientes).
 *
 * <p>A entidade tem comportamento ({@link #updateDetails}, {@link #deactivate}) em vez de setters
 * publicos: so e possivel mudar o estado por operacoes com significado de negocio.
 * O construtor sem argumentos e {@code protected} porque existe apenas para o Hibernate.</p>
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** CPF armazenado apenas com digitos. */
    @Column(nullable = false, unique = true, length = 11)
    private String document;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Controle de concorrencia otimista: evita que uma atualizacao sobrescreva outra silenciosamente. */
    @Version
    private Long version;

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email, String document, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.document = document;
        this.phone = phone;
        this.active = true;
    }

    public void updateDetails(String newFirstName, String newLastName, String newEmail, String newPhone) {
        this.firstName = newFirstName;
        this.lastName = newLastName;
        this.email = newEmail;
        this.phone = newPhone;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDocument() {
        return document;
    }

    public String getPhone() {
        return phone;
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

    public Long getVersion() {
        return version;
    }

    /**
     * Igualdade por identificador: duas instancias representam o mesmo cliente se tem o mesmo id.
     * Entidades ainda nao persistidas (id nulo) so sao iguais a si mesmas.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
