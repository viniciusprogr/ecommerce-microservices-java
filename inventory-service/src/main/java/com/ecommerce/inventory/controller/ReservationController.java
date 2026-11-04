package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryDtos.ReservationRequest;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.ecommerce.inventory.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API INTERNA de reservas, usada pelo Order Service.
 * O API Gateway bloqueia este caminho para clientes externos (ADR-0005).
 */
@RestController
@RequestMapping("/api/inventory/reservations")
@Tag(name = "Reservations (internal)", description = "Reserva, baixa e liberacao de estoque - uso interno do Order Service")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva estoque para um pedido (idempotente pela reference)")
    public ReservationResponse reserve(@Valid @RequestBody ReservationRequest request) {
        return service.reserve(request);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Consulta uma reserva")
    public ReservationResponse find(@PathVariable String reference) {
        return service.findByReference(reference);
    }

    @PostMapping("/{reference}/commit")
    @Operation(summary = "Baixa definitiva do estoque reservado (pedido enviado)")
    public ReservationResponse commit(@PathVariable String reference) {
        return service.commit(reference);
    }

    @PostMapping("/{reference}/release")
    @Operation(summary = "Libera o estoque reservado (pedido cancelado)")
    public ReservationResponse release(@PathVariable String reference) {
        return service.release(reference);
    }
}
