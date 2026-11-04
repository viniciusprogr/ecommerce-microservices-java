package com.ecommerce.order.client;

import com.ecommerce.order.client.RemoteDtos.RemoteProduct;
import com.ecommerce.order.exception.InvalidOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/** Cliente HTTP do Product Service. */
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @throws InvalidOrderException se o produto nao existir (422)
     */
    public RemoteProduct getProduct(Long productId) {
        return RemoteCalls.execute("product-service", () -> restClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                    throw new InvalidOrderException("Product " + productId + " does not exist");
                })
                .body(RemoteProduct.class));
    }
}
