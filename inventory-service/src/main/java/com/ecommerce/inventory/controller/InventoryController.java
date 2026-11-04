package com.ecommerce.inventory.controller;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.inventory.dto.InventoryDtos.AvailabilityResponse;
import com.ecommerce.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.ecommerce.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.ecommerce.inventory.dto.InventoryDtos.StockAdjustmentRequest;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Endpoints de consulta e manutencao de estoque. */
@Validated
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Controle de estoque")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria o registro de estoque de um produto (ADMIN)")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItemResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{productId}")
                .buildAndExpand(created.productId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista as posicoes de estoque")
    public PageResponse<InventoryItemResponse> findAll(
            @PageableDefault(size = 20, sort = "productId", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Consulta o estoque de um produto")
    public InventoryItemResponse findByProductId(@PathVariable Long productId) {
        return service.findByProductId(productId);
    }

    @GetMapping("/{productId}/availability")
    @Operation(summary = "Verifica se ha quantidade disponivel")
    public AvailabilityResponse availability(@PathVariable Long productId,
                                             @RequestParam(defaultValue = "1") @Positive int quantity) {
        return service.checkAvailability(productId, quantity);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Ajusta a quantidade fisica em estoque (ADMIN)")
    public InventoryItemResponse adjust(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return service.adjustOnHand(productId, request.quantityOnHand());
    }
}
