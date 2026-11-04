package com.ecommerce.order.client;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.RemoteServiceException;
import com.ecommerce.order.client.RemoteDtos.ReservationItem;
import com.ecommerce.order.client.RemoteDtos.ReservationRequest;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.InvalidOrderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testa o contrato HTTP de cada cliente com {@link MockRestServiceServer}: verificamos a URL e o corpo enviados
 * e como cada resposta (200, 404, 409, 500, timeout) e traduzida para o dominio de pedidos.
 */
class RemoteClientsTest {

    private static final String BASE = "http://remote";

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl(BASE);
    }

    @Test
    void customerClientShouldParseCustomerIgnoringUnknownFields() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/customers/1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1,"firstName":"Maria","lastName":"Silva","email":"m@e.com","active":true,"document":"x"}
                        """, MediaType.APPLICATION_JSON));

        var customer = new CustomerClient(builder.build()).getCustomer(1L);

        assertThat(customer.active()).isTrue();
        assertThat(customer.firstName()).isEqualTo("Maria");
        server.verify();
    }

    @Test
    void customerClientShouldMap404ToInvalidOrder() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/customers/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> new CustomerClient(builder.build()).getCustomer(9L))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Customer 9 does not exist");
    }

    @Test
    void productClientShouldMap404And500() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/products/1")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE + "/api/products/2")).andRespond(withServerError());
        ProductClient client = new ProductClient(builder.build());

        assertThatThrownBy(() -> client.getProduct(1L)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> client.getProduct(2L))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessage("product-service responded with status 500");
    }

    @Test
    void productClientShouldMapConnectionFailureToRemoteServiceException() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/products/1")).andRespond(withException(new IOException("refused")));

        assertThatThrownBy(() -> new ProductClient(builder.build()).getProduct(1L))
                .isInstanceOf(RemoteServiceException.class)
                .hasMessage("product-service is unavailable");
    }

    @Test
    void inventoryClientShouldSendReservationAndMapConflict() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/inventory/reservations")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"reference\":\"o-1\",\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andRespond(withSuccess("{\"reference\":\"o-1\",\"status\":\"RESERVED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/inventory/reservations"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409,\"message\":\"Insufficient stock: product 1 requested 2, available 0\"}"));
        InventoryClient client = new InventoryClient(builder.build(), new ObjectMapper());
        var request = new ReservationRequest("o-1", List.of(new ReservationItem(1L, 2)));

        assertThat(client.reserve(request).status()).isEqualTo("RESERVED");
        assertThatThrownBy(() -> client.reserve(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock: product 1 requested 2, available 0");
        server.verify();
    }

    @Test
    void inventoryClientShouldCommitReleaseAndMapConflictWithoutBody() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/api/inventory/reservations/o-1/commit"))
                .andRespond(withSuccess("{\"reference\":\"o-1\",\"status\":\"COMMITTED\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/inventory/reservations/o-1/release"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        InventoryClient client = new InventoryClient(builder.build(), new ObjectMapper());

        assertThat(client.commit("o-1").status()).isEqualTo("COMMITTED");
        assertThatThrownBy(() -> client.release("o-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot release reservation o-1");
    }
}
