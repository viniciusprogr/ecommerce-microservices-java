package com.ecommerce.customer.controller;

import com.ecommerce.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao dos endpoints: contexto Spring completo, seguranca real, H2 + Flyway.
 *
 * <p>{@code jwt()} do spring-security-test cria uma autenticacao JWT ja validada, com as roles desejadas,
 * sem precisar gerar e assinar um token de verdade.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    private static final String BODY = """
            {"firstName":"Maria","lastName":"Silva","email":"maria@email.com",
             "document":"529.982.247-25","phone":"11987654321"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private long createCustomer() throws Exception {
        String location = mockMvc.perform(post("/api/customers").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void shouldCreateAndFetchCustomer() throws Exception {
        mockMvc.perform(post("/api/customers").with(user()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/customers/")))
                .andExpect(jsonPath("$.document").value("52998224725"))
                .andExpect(jsonPath("$.active").value(true));

        long id = repository.findAll().getFirst().getId();
        mockMvc.perform(get("/api/customers/{id}", id).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    void shouldReturn400WithViolations_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/customers").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Silva\",\"email\":\"bad\",\"document\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.length()").value(3));
    }

    @Test
    void shouldReturn409_whenEmailIsDuplicated() throws Exception {
        createCustomer();

        mockMvc.perform(post("/api/customers").with(user()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404_whenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/customers/999").with(user()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer 999 not found"));
    }

    @Test
    void shouldListWithPagination() throws Exception {
        createCustomer();

        mockMvc.perform(get("/api/customers?page=0&size=5&active=true").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        long id = createCustomer();

        mockMvc.perform(put("/api/customers/{id}", id).with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("Maria", "Mariana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Mariana"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldOnlyAllowAdminToDeactivateAndActivate() throws Exception {
        long id = createCustomer();

        mockMvc.perform(delete("/api/customers/{id}", id).with(user())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/customers/{id}", id).with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/customers/{id}", id).with(admin())).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch("/api/customers/{id}/activate", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldExposeHealthAndApiDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Customer Service API"));
    }

    @Test
    void shouldEchoCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/customers").with(user()).header("X-Correlation-Id", "test-123"))
                .andExpect(header().string("X-Correlation-Id", "test-123"));
    }
}
