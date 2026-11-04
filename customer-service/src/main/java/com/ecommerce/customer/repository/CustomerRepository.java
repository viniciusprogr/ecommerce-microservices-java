package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de clientes (Repository Pattern via Spring Data JPA).
 *
 * <p>O Spring gera a implementacao em tempo de execucao a partir do nome dos metodos
 * ("query derivation"): {@code existsByEmail} vira {@code select count(*) > 0 ... where email = ?}.</p>
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Customer> findByActive(boolean active, Pageable pageable);
}
