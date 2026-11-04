package com.ecommerce.product.controller;

import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private long createCategory(String name) throws Exception {
        String body = mockMvc.perform(post("/api/categories").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createProduct(long categoryId) throws Exception {
        String body = mockMvc.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("MOUSE-001", "99.90", categoryId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String productJson(String sku, String price, long categoryId) {
        return """
                {"sku":"%s","name":"Mouse","description":"Optico","price":%s,"categoryId":%d}
                """.formatted(sku, price, categoryId);
    }

    @Test
    void shouldManageProductLifecycle() throws Exception {
        long categoryId = createCategory("Eletronicos");
        long productId = createProduct(categoryId);

        mockMvc.perform(get("/api/products/{id}", productId).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Eletronicos"))
                .andExpect(jsonPath("$.price").value(99.90));

        mockMvc.perform(put("/api/products/{id}", productId).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("MOUSE-001", "79.90", categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(79.90));

        mockMvc.perform(delete("/api/products/{id}", productId).with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products?active=false").with(user()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(patch("/api/products/{id}/activate", productId).with(admin()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldFilterProductsByName() throws Exception {
        createProduct(createCategory("Eletronicos"));

        mockMvc.perform(get("/api/products?name=mou").with(user()))
                .andExpect(jsonPath("$.content[0].sku").value("MOUSE-001"));
        mockMvc.perform(get("/api/products?name=teclado").with(user()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectInvalidProduct() throws Exception {
        mockMvc.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("lower-case", "0", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    void shouldReturn404_whenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("MOUSE-001", "10.00", 999)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409_forDuplicatedSkuAndCategoryInUse() throws Exception {
        long categoryId = createCategory("Eletronicos");
        createProduct(categoryId);

        mockMvc.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("MOUSE-001", "10.00", categoryId)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(admin())).andExpect(status().isConflict());
    }

    @Test
    void shouldManageCategories() throws Exception {
        long id = createCategory("Livros");

        mockMvc.perform(get("/api/categories").with(user())).andExpect(jsonPath("$[0].name").value("Livros"));
        mockMvc.perform(put("/api/categories/{id}", id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Livros Tecnicos\"}")).andExpect(jsonPath("$.name").value("Livros Tecnicos"));
        mockMvc.perform(get("/api/categories/{id}", id).with(user())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/categories/{id}", id).with(admin())).andExpect(status().isNoContent());
    }

    @Test
    void shouldForbidWritesForRegularUsers() throws Exception {
        mockMvc.perform(post("/api/categories").with(user()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }
}
