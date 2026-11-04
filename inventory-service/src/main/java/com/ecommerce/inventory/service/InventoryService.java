package com.ecommerce.inventory.service;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.inventory.dto.InventoryDtos.AvailabilityResponse;
import com.ecommerce.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.ecommerce.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.ecommerce.inventory.entity.InventoryItem;
import com.ecommerce.inventory.exception.DuplicateInventoryItemException;
import com.ecommerce.inventory.exception.InventoryItemNotFoundException;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta e manutencao das posicoes de estoque. */
@Service
public class InventoryService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository repository;
    private final InventoryMapper mapper;

    public InventoryService(InventoryItemRepository repository, InventoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        if (repository.existsByProductId(request.productId())) {
            throw new DuplicateInventoryItemException(request.productId());
        }
        InventoryItem saved = repository.save(new InventoryItem(request.productId(), request.quantity()));
        LOG.info("Inventory created productId={} quantity={}", request.productId(), request.quantity());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findByProductId(Long productId) {
        return mapper.toResponse(getItem(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long productId, int quantity) {
        InventoryItem item = getItem(productId);
        return new AvailabilityResponse(productId, quantity, item.available(), item.canReserve(quantity));
    }

    @Transactional
    public InventoryItemResponse adjustOnHand(Long productId, int newQuantity) {
        InventoryItem item = getItem(productId);
        item.adjustOnHand(newQuantity);
        LOG.info("Inventory adjusted productId={} onHand={}", productId, newQuantity);
        return mapper.toResponse(repository.saveAndFlush(item));
    }

    private InventoryItem getItem(Long productId) {
        return repository.findByProductId(productId).orElseThrow(() -> new InventoryItemNotFoundException(productId));
    }
}
