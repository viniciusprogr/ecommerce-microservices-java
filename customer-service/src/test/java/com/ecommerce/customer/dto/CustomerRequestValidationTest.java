package com.ecommerce.customer.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa as regras do Bean Validation diretamente com o {@link Validator}, sem Spring.
 */
class CustomerRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void shouldAcceptValidRequest() {
        var request = new CustomerRequest("Maria", "Silva", "maria@email.com", "529.982.247-25", "11987654321");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldAcceptMissingPhone() {
        var request = new CustomerRequest("Maria", "Silva", "maria@email.com", "52998224725", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectBlankNamesAndInvalidEmail() {
        var request = new CustomerRequest(" ", "", "not-an-email", "52998224725", null);

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("firstName", "lastName", "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789-00", "11111111111", "abc"})
    void shouldRejectInvalidCpf(String cpf) {
        var request = new CustomerRequest("Maria", "Silva", "maria@email.com", cpf, null);

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("document");
    }

    @Test
    void shouldRejectInvalidPhone() {
        var request = new CustomerRequest("Maria", "Silva", "maria@email.com", "52998224725", "call me");

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("phone");
    }
}
