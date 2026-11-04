package com.ecommerce.customer.controller;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.customer.dto.CustomerRequest;
import com.ecommerce.customer.dto.CustomerResponse;
import com.ecommerce.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints REST de clientes. O controller apenas traduz HTTP para chamadas ao service:
 * nenhuma regra de negocio mora aqui.
 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Cadastro e gerenciamento de clientes")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    public CustomerResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    @Operation(summary = "Lista clientes paginados, com filtro opcional por status")
    public PageResponse<CustomerResponse> findAll(@RequestParam(required = false) Boolean active,
                                                  @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
                                                  Pageable pageable) {
        return service.findAll(active, pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um cliente (o CPF nao pode mudar)")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reativa um cliente (ADMIN)")
    public CustomerResponse activate(@PathVariable Long id) {
        return service.activate(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa um cliente - exclusao logica (ADMIN)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
