package com.ecommerce.customer.mapper;

import com.ecommerce.customer.dto.CustomerRequest;
import com.ecommerce.customer.dto.CustomerResponse;
import com.ecommerce.customer.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Converte entre a entidade {@link Customer} e os DTOs.
 *
 * <p>Tambem normaliza dados de entrada (e-mail em minusculas, CPF so com digitos) para que
 * a verificacao de duplicidade funcione independentemente da forma como o usuario digitou.</p>
 */
@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        return new Customer(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeEmail(request.email()),
                normalizeDocument(request.document()),
                trimToNull(request.phone()));
    }

    public void updateEntity(Customer customer, CustomerRequest request) {
        customer.updateDetails(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeEmail(request.email()),
                trimToNull(request.phone()));
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getDocument(),
                customer.getPhone(),
                customer.isActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeDocument(String document) {
        return document.replaceAll("\\D", "");
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
