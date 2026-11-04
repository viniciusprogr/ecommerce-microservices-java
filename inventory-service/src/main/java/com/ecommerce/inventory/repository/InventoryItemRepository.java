package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de estoque.
 *
 * <p>{@link #findAllForUpdate} usa bloqueio PESSIMISTA ({@code SELECT ... FOR UPDATE}): duas reservas
 * simultaneas do mesmo produto sao serializadas pelo banco, entao nao vendemos a mesma unidade duas vezes.
 * Ordenar por {@code productId} faz todas as transacoes travarem as linhas na mesma ordem, evitando deadlocks.</p>
 */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId in :productIds order by i.productId")
    List<InventoryItem> findAllForUpdate(@Param("productIds") Collection<Long> productIds);
}
