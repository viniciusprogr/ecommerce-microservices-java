package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de repositorio com {@code @DataJpaTest}: sobe apenas a camada JPA (H2 + Flyway),
 * e cada teste roda em uma transacao que sofre rollback ao final.
 * {@code replace = NONE} faz o teste usar a URL do perfil "test" (H2 em modo PostgreSQL).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Test
    void shouldPersistWithAuditFields() {
        Customer saved = repository.saveAndFlush(new Customer("Ana", "Souza", "ana@email.com", "52998224725", null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void shouldCheckDuplicates() {
        Customer saved = repository.saveAndFlush(new Customer("Ana", "Souza", "ana@email.com", "52998224725", null));

        assertThat(repository.existsByEmail("ana@email.com")).isTrue();
        assertThat(repository.existsByDocument("52998224725")).isTrue();
        assertThat(repository.existsByEmailAndIdNot("ana@email.com", saved.getId())).isFalse();
        assertThat(repository.existsByEmail("other@email.com")).isFalse();
    }

    @Test
    void shouldFilterByActive() {
        repository.save(new Customer("Ana", "Souza", "ana@email.com", "52998224725", null));
        Customer inactive = new Customer("Bia", "Lima", "bia@email.com", "11144477735", null);
        inactive.deactivate();
        repository.save(inactive);

        assertThat(repository.findByActive(true, PageRequest.of(0, 10)).getContent())
                .extracting(Customer::getEmail).containsExactly("ana@email.com");
        assertThat(repository.findByActive(false, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldEnforceUniqueEmailAtDatabaseLevel() {
        repository.saveAndFlush(new Customer("Ana", "Souza", "ana@email.com", "52998224725", null));

        assertThatThrownBy(() -> repository.saveAndFlush(new Customer("Ana", "Clone", "ana@email.com", "11144477735", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
