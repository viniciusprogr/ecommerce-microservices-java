package com.ecommerce.order.controller;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.order.dto.OrderDtos.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.ecommerce.order.dto.OrderDtos.UpdateOrderStatusRequest;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Endpoints de pedidos. */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Criacao, consulta e atualizacao de status de pedidos")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um pedido (valida cliente e produtos e reserva estoque)")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pedido")
    public OrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtros opcionais por cliente e status")
    public PageResponse<OrderResponse> findAll(@RequestParam(required = false) Long customerId,
                                               @RequestParam(required = false) OrderStatus status,
                                               @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
                                               Pageable pageable) {
        return service.findAll(customerId, status, pageable);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status do pedido (ADMIN)")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
