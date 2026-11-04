package com.ecommerce.order;

import com.ecommerce.order.repository.OrderRepository;
import com.jayway.jsonpath.JsonPath;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao da COMUNICACAO ENTRE SERVICOS.
 *
 * <p>Sobe o Order Service completo (HTTP, seguranca, JPA/H2) e substitui Customer, Product e Inventory por um
 * servidor HTTP falso ({@link MockWebServer}). Diferente de mocks do Mockito, aqui as requisicoes realmente
 * trafegam pela rede local: validamos URLs, JSON, propagacao do token JWT e do Correlation ID.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    private static final MockWebServer REMOTE = new MockWebServer();
    private static final List<RecordedRequest> RECEIVED = new CopyOnWriteArrayList<>();
    private static volatile int availableStock = 10;

    static {
        REMOTE.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                RECEIVED.add(request);
                return route(request);
            }
        });
        try {
            REMOTE.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void remoteUrls(DynamicPropertyRegistry registry) {
        String url = REMOTE.url("/").toString().replaceAll("/$", "");
        registry.add("services.customer-url", () -> url);
        registry.add("services.product-url", () -> url);
        registry.add("services.inventory-url", () -> url);
    }

    @AfterAll
    static void stopServer() throws IOException {
        REMOTE.shutdown();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository repository;

    @BeforeEach
    void reset() {
        repository.deleteAll();
        RECEIVED.clear();
        availableStock = 10;
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body);
    }

    private static MockResponse route(RecordedRequest request) {
        String path = request.getPath();
        if ("/api/customers/1".equals(path)) {
            return json(200, "{\"id\":1,\"firstName\":\"Maria\",\"lastName\":\"Silva\",\"email\":\"m@e.com\",\"active\":true}");
        }
        if ("/api/customers/404".equals(path)) {
            return json(404, "{\"status\":404,\"message\":\"Customer 404 not found\"}");
        }
        if ("/api/products/10".equals(path)) {
            return json(200, "{\"id\":10,\"sku\":\"MOUSE-001\",\"name\":\"Mouse\",\"price\":59.90,\"active\":true}");
        }
        if ("/api/inventory/reservations".equals(path)) {
            String body = request.getBody().readUtf8();
            int quantity = ((Number) JsonPath.read(body, "$.items[0].quantity")).intValue();
            if (quantity > availableStock) {
                return json(409, "{\"status\":409,\"message\":\"Insufficient stock: product 10\"}");
            }
            String reference = JsonPath.read(body, "$.reference");
            return json(201, "{\"reference\":\"" + reference + "\",\"status\":\"RESERVED\"}");
        }
        if (path != null && path.endsWith("/commit")) {
            return json(200, "{\"status\":\"COMMITTED\"}");
        }
        return json(500, "{}");
    }

    private static RequestPostProcessor user() {
        return jwt().jwt(token -> token.tokenValue("user-token").subject("maria"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.tokenValue("admin-token"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static final String ORDER = "{\"customerId\":1,\"items\":[{\"productId\":10,\"quantity\":3}]}";

    @Test
    void shouldCreateOrderCallingAllServicesAndPropagatingHeaders() throws Exception {
        String body = mockMvc.perform(post("/api/orders").with(user()).header("X-Correlation-Id", "flow-123")
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(179.70))
                .andExpect(jsonPath("$.items[0].productName").value("Mouse"))
                .andReturn().getResponse().getContentAsString();

        assertThat(RECEIVED).extracting(RecordedRequest::getPath)
                .containsExactly("/api/customers/1", "/api/products/10", "/api/inventory/reservations");
        assertThat(RECEIVED).allSatisfy(request -> {
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer user-token");
            assertThat(request.getHeader("X-Correlation-Id")).isEqualTo("flow-123");
        });

        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mockMvc.perform(get("/api/orders/{id}", id).with(user())).andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/orders?customerId=1&status=CONFIRMED").with(user()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn409_whenInventoryHasNoStock() throws Exception {
        availableStock = 1;

        mockMvc.perform(post("/api/orders").with(user()).contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient stock: product 10"));
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturn422_whenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/orders").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":404,\"items\":[{\"productId\":10,\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn503_whenDependencyFails() throws Exception {
        mockMvc.perform(post("/api/orders").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"productId\":77,\"quantity\":1}]}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldShipOrderAsAdminCommittingStock() throws Exception {
        String body = mockMvc.perform(post("/api/orders").with(user()).contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        String orderNumber = JsonPath.read(body, "$.orderNumber");

        mockMvc.perform(patch("/api/orders/{id}/status", id).with(user()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHIPPED\"}")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/orders/{id}/status", id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(patch("/api/orders/{id}/status", id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isConflict());

        assertThat(RECEIVED).extracting(RecordedRequest::getPath)
                .contains("/api/inventory/reservations/" + orderNumber + "/commit");
    }

    @Test
    void shouldValidateRequests() throws Exception {
        mockMvc.perform(post("/api/orders").with(user()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"items\":[]}")).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/orders/1/status").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FLYING\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders/999").with(user())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        assertThat(RECEIVED).isEmpty();
    }
}
