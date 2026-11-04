package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.BusinessRuleException;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationItem;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationRequest;
import com.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.ecommerce.inventory.entity.InventoryItem;
import com.ecommerce.inventory.entity.Reservation;
import com.ecommerce.inventory.entity.ReservationLine;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.ReservationNotFoundException;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserva, baixa (commit) e liberacao (release) de estoque.
 *
 * <p>Cada operacao roda em uma unica transacao local: ou todos os itens sao reservados,
 * ou nenhum e ("tudo ou nada").</p>
 */
@Service
public class ReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryMapper mapper;

    public ReservationService(ReservationRepository reservationRepository, InventoryItemRepository itemRepository,
                              InventoryMapper mapper) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.mapper = mapper;
    }

    /**
     * Reserva todos os itens ou nenhum. Idempotente: a mesma {@code reference} devolve a reserva existente.
     */
    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        Optional<Reservation> existing = reservationRepository.findByReference(request.reference());
        if (existing.isPresent()) {
            LOG.info("Reservation {} already exists, returning it (idempotent)", request.reference());
            return mapper.toResponse(existing.get());
        }
        Map<Long, Integer> requested = toQuantityByProduct(request.items());
        Map<Long, InventoryItem> items = lockItems(requested.keySet());

        List<String> shortages = new ArrayList<>();
        requested.forEach((productId, quantity) -> {
            InventoryItem item = items.get(productId);
            if (item == null) {
                shortages.add("product " + productId + " has no inventory");
            } else if (!item.canReserve(quantity)) {
                shortages.add("product " + productId + " requested " + quantity + ", available " + item.available());
            }
        });
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        Reservation reservation = new Reservation(request.reference());
        requested.forEach((productId, quantity) -> {
            items.get(productId).reserve(quantity);
            reservation.addLine(productId, quantity);
        });
        Reservation saved = reservationRepository.save(reservation);
        LOG.info("Reservation {} created with {} items", saved.getReference(), requested.size());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findByReference(String reference) {
        return mapper.toResponse(getReservation(reference));
    }

    /** Baixa definitiva (pedido enviado). Idempotente. */
    @Transactional
    public ReservationResponse commit(String reference) {
        Reservation reservation = getReservation(reference);
        if (reservation.markCommitted()) {
            Map<Long, InventoryItem> items = lockItems(productIds(reservation));
            reservation.getLines().forEach(line -> items.get(line.getProductId()).commit(line.getQuantity()));
            LOG.info("Reservation {} committed", reference);
        }
        return mapper.toResponse(reservationRepository.saveAndFlush(reservation));
    }

    /** Devolve o estoque reservado (pedido cancelado ou compensacao). Idempotente. */
    @Transactional
    public ReservationResponse release(String reference) {
        Reservation reservation = getReservation(reference);
        if (reservation.markReleased()) {
            Map<Long, InventoryItem> items = lockItems(productIds(reservation));
            reservation.getLines().forEach(line -> items.get(line.getProductId()).release(line.getQuantity()));
            LOG.info("Reservation {} released", reference);
        }
        return mapper.toResponse(reservationRepository.saveAndFlush(reservation));
    }

    private Map<Long, Integer> toQuantityByProduct(List<ReservationItem> items) {
        Map<Long, Integer> result = items.stream()
                .collect(Collectors.toMap(ReservationItem::productId, ReservationItem::quantity, (a, b) -> -1));
        if (result.containsValue(-1)) {
            throw new BusinessRuleException("Each product can appear only once in a reservation");
        }
        return result;
    }

    private Map<Long, InventoryItem> lockItems(Collection<Long> productIds) {
        return itemRepository.findAllForUpdate(productIds).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
    }

    private static List<Long> productIds(Reservation reservation) {
        return reservation.getLines().stream().map(ReservationLine::getProductId).toList();
    }

    private Reservation getReservation(String reference) {
        return reservationRepository.findByReference(reference)
                .orElseThrow(() -> new ReservationNotFoundException(reference));
    }
}
