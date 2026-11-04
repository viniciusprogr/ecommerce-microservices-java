package com.ecommerce.gateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integracao do gateway: servidor real em porta aleatoria, seguranca real e um "microsservico" falso
 * ({@link MockWebServer}) como destino de todas as rotas. Valida login, protecao de rotas e o proxy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    private static final MockWebServer BACKEND = new MockWebServer();

    static {
        try {
            BACKEND.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        String url = BACKEND.url("/").toString().replaceAll("/$", "");
        registry.add("CUSTOMER_SERVICE_URL", () -> url);
        registry.add("PRODUCT_SERVICE_URL", () -> url);
        registry.add("INVENTORY_SERVICE_URL", () -> url);
        registry.add("ORDER_SERVICE_URL", () -> url);
    }

    @AfterAll
    static void stop() throws IOException {
        BACKEND.shutdown();
    }

    @Autowired
    private WebTestClient client;

    private String login(String username, String password) {
        Map<?, ?> body = client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        assertThat(body).isNotNull();
        return (String) body.get("accessToken");
    }

    @Test
    void shouldIssueTokenAndProxyAuthenticatedRequest() throws Exception {
        String token = login("user", "user-test-password");
        BACKEND.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{\"content\":[]}"));

        client.get().uri("/api/customers?page=0").header("Authorization", "Bearer " + token)
                .header("X-Correlation-Id", "gw-test-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "gw-test-1")
                .expectBody().jsonPath("$.content").isArray();

        RecordedRequest forwarded = BACKEND.takeRequest(5, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getPath()).isEqualTo("/api/customers?page=0");
        assertThat(forwarded.getHeader("Authorization")).isEqualTo("Bearer " + token);
        assertThat(forwarded.getHeader("X-Correlation-Id")).isEqualTo("gw-test-1");
    }

    @Test
    void shouldGenerateCorrelationId_whenMissing() throws Exception {
        String token = login("admin", "admin-test-password");
        BACKEND.enqueue(new MockResponse().setResponseCode(201).setBody("{}"));

        client.post().uri("/api/orders").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("X-Correlation-Id");

        RecordedRequest forwarded = BACKEND.takeRequest(5, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader("X-Correlation-Id")).hasSize(36);
    }

    @Test
    void shouldReturnRolesInLoginResponse() {
        client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "admin", "password", "admin-test-password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.roles[0]").isEqualTo("ADMIN")
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    void shouldRejectInvalidCredentialsWithGenericMessage() {
        client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "user", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Invalid username or password");
        client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "ghost", "password", "x"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectBlankCredentials() {
        client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "", "password", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRequireTokenForApiRoutes() {
        client.get().uri("/api/products").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectBody().jsonPath("$.status").isEqualTo(401);
        client.get().uri("/api/products").header("Authorization", "Bearer not-a-jwt").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldBlockInternalReservationApi() {
        String token = login("admin", "admin-test-password");

        client.post().uri("/api/inventory/reservations").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.status").isEqualTo(403);
    }

    @Test
    void shouldExposeHealthAndDenyUnknownPaths() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
        client.get().uri("/something-else").exchange().expectStatus().isUnauthorized();
    }
}
