package com.ecommerce.customer;

import com.ecommerce.customer.dto.CustomerRequest;
import com.ecommerce.customer.exception.DuplicateCustomerException;
import com.ecommerce.customer.repository.CustomerRepository;
import com.ecommerce.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integracao com PostgreSQL REAL (Testcontainers).
 *
 * <p>Sobe um container {@code postgres:16-alpine}, executa as migrations Flyway e valida o esquema
 * com o Hibernate ({@code ddl-auto=validate}). {@code @ServiceConnection} injeta a URL, usuario e senha
 * do container no Spring, sobrescrevendo o H2 do perfil de testes. Requer Docker (roda no CI).</p>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CustomerPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerService service;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shouldRunMigrationsAndPersistOnPostgres() {
        var created = service.create(new CustomerRequest("Maria", "Silva", "maria@email.com", "529.982.247-25", null));

        assertThat(created.id()).isPositive();
        assertThat(service.findAll(true, PageRequest.of(0, 10)).totalElements()).isEqualTo(1);
    }

    @Test
    void shouldDetectDuplicateDocumentOnPostgres() {
        service.create(new CustomerRequest("Maria", "Silva", "maria@email.com", "52998224725", null));

        assertThatThrownBy(() -> service.create(new CustomerRequest("Outra", "Pessoa", "x@email.com", "52998224725", null)))
                .isInstanceOf(DuplicateCustomerException.class);
    }
}
