package com.ecommerce.customer.service;

import com.ecommerce.common.exception.BusinessRuleException;
import com.ecommerce.common.web.PageResponse;
import com.ecommerce.customer.dto.CustomerRequest;
import com.ecommerce.customer.dto.CustomerResponse;
import com.ecommerce.customer.entity.Customer;
import com.ecommerce.customer.exception.CustomerNotFoundException;
import com.ecommerce.customer.exception.DuplicateCustomerException;
import com.ecommerce.customer.mapper.CustomerMapper;
import com.ecommerce.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios do {@link CustomerService}.
 *
 * <p>O repositorio e substituido por um mock do Mockito: testamos apenas a regra de negocio,
 * sem banco de dados. O mapper e real porque e uma classe simples, sem dependencias.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String VALID_CPF = "529.982.247-25";

    @Mock
    private CustomerRepository repository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, new CustomerMapper());
    }

    private static CustomerRequest request(String email) {
        return new CustomerRequest(" Maria ", "Silva", email, VALID_CPF, "+55 11 98765-4321");
    }

    private static Customer persistedCustomer(Long id) {
        Customer customer = new Customer("Maria", "Silva", "maria@email.com", "52998224725", null);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    @Nested
    class Create {

        @Test
        void shouldNormalizeAndSave_whenDataIsUnique() {
            when(repository.existsByEmail("maria@email.com")).thenReturn(false);
            when(repository.existsByDocument("52998224725")).thenReturn(false);
            when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
                Customer saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            CustomerResponse response = service.create(request("  MARIA@Email.com "));

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("maria@email.com");
            assertThat(captor.getValue().getDocument()).isEqualTo("52998224725");
            assertThat(captor.getValue().getFirstName()).isEqualTo("Maria");
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.active()).isTrue();
        }

        @Test
        void shouldRejectDuplicatedEmail() {
            when(repository.existsByEmail("maria@email.com")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request("maria@email.com")))
                    .isInstanceOf(DuplicateCustomerException.class)
                    .hasMessageContaining("email");
            verify(repository, never()).save(any());
        }

        @Test
        void shouldRejectDuplicatedDocument() {
            when(repository.existsByEmail("maria@email.com")).thenReturn(false);
            when(repository.existsByDocument("52998224725")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request("maria@email.com")))
                    .isInstanceOf(DuplicateCustomerException.class)
                    .hasMessageContaining("document");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Read {

        @Test
        void shouldReturnCustomer_whenFound() {
            when(repository.findById(1L)).thenReturn(Optional.of(persistedCustomer(1L)));

            assertThat(service.findById(1L).email()).isEqualTo("maria@email.com");
        }

        @Test
        void shouldThrowNotFound_whenMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer 99 not found");
        }

        @Test
        void shouldFilterByActive_whenFilterIsProvided() {
            var pageable = PageRequest.of(0, 10);
            when(repository.findByActive(true, pageable))
                    .thenReturn(new PageImpl<>(List.of(persistedCustomer(1L)), pageable, 1));

            PageResponse<CustomerResponse> page = service.findAll(true, pageable);

            assertThat(page.totalElements()).isEqualTo(1);
            verify(repository, never()).findAll(pageable);
        }

        @Test
        void shouldListAll_whenNoFilter() {
            var pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            assertThat(service.findAll(null, pageable).content()).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateDetails() {
            Customer existing = persistedCustomer(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByEmailAndIdNot("new@email.com", 1L)).thenReturn(false);
            when(repository.saveAndFlush(existing)).thenReturn(existing);

            CustomerResponse response = service.update(1L, request("new@email.com"));

            assertThat(response.email()).isEqualTo("new@email.com");
            assertThat(existing.getPhone()).isEqualTo("+55 11 98765-4321");
        }

        @Test
        void shouldRejectEmailUsedByAnotherCustomer() {
            when(repository.findById(1L)).thenReturn(Optional.of(persistedCustomer(1L)));
            when(repository.existsByEmailAndIdNot("taken@email.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(1L, request("taken@email.com")))
                    .isInstanceOf(DuplicateCustomerException.class);
        }

        @Test
        void shouldRejectDocumentChange() {
            when(repository.findById(1L)).thenReturn(Optional.of(persistedCustomer(1L)));
            var changedCpf = new CustomerRequest("Maria", "Silva", "maria@email.com", "111.444.777-35", null);

            assertThatThrownBy(() -> service.update(1L, changedCpf))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be changed");
        }
    }

    @Nested
    class Activation {

        @Test
        void shouldDeactivateCustomer() {
            Customer existing = persistedCustomer(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            service.deactivate(1L);

            assertThat(existing.isActive()).isFalse();
        }

        @Test
        void shouldReactivateCustomer() {
            Customer existing = persistedCustomer(1L);
            existing.deactivate();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.saveAndFlush(existing)).thenReturn(existing);

            assertThat(service.activate(1L).active()).isTrue();
        }

        @Test
        void shouldThrowNotFound_whenDeactivatingMissingCustomer() {
            when(repository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(5L)).isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
