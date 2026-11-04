package com.ecommerce.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representacoes LOCAIS dos dados de outros servicos.
 *
 * <p>Nao importamos classes do Customer/Product/Inventory: cada servico declara apenas os campos de que
 * precisa (padrao "Tolerant Reader"). Campos extras no JSON sao ignorados, entao os outros servicos podem
 * evoluir sem quebrar o Order Service.</p>
 */
public final class RemoteDtos {

    private RemoteDtos() {
    }

    public record RemoteCustomer(Long id, String firstName, String lastName, String email, boolean active) {
    }

    public record RemoteProduct(Long id, String sku, String name, BigDecimal price, boolean active) {
    }

    public record ReservationItem(Long productId, int quantity) {
    }

    public record ReservationRequest(String reference, List<ReservationItem> items) {
    }

    public record ReservationResponse(String reference, String status) {
    }
}
