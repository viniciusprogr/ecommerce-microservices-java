package com.ecommerce.order.client;

import com.ecommerce.order.client.RemoteDtos.RemoteCustomer;
import com.ecommerce.order.exception.InvalidOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/** Cliente HTTP do Customer Service. */
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @throws InvalidOrderException se o cliente nao existir (o pedido referencia algo invalido: 422)
     */
    public RemoteCustomer getCustomer(Long customerId) {
        return RemoteCalls.execute("customer-service", () -> restClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                    throw new InvalidOrderException("Customer " + customerId + " does not exist");
                })
                .body(RemoteCustomer.class));
    }
}
