package com.ecommerce.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o mapeamento excecao -> status HTTP usando um controller "falso" e o MockMvc
 * em modo standalone (sem subir o contexto Spring inteiro: rapido e isolado).
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldMapNotFoundTo404() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("thing 1 not found"))
                .andExpect(jsonPath("$.path").value("/fake/not-found"));
    }

    @Test
    void shouldMapConflictTo409() throws Exception {
        mockMvc.perform(get("/fake/conflict")).andExpect(status().isConflict());
    }

    @Test
    void shouldMapBusinessRuleTo422() throws Exception {
        mockMvc.perform(get("/fake/business")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldMapRemoteServiceTo503() throws Exception {
        mockMvc.perform(get("/fake/remote")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldMapDataIntegrityAndOptimisticLockTo409() throws Exception {
        mockMvc.perform(get("/fake/integrity")).andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("SQL"))));
        mockMvc.perform(get("/fake/optimistic")).andExpect(status().isConflict());
    }

    @Test
    void shouldMapAccessDeniedTo403() throws Exception {
        mockMvc.perform(get("/fake/denied")).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnFieldViolations_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/fake/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("name"));
    }

    @Test
    void shouldReturn400_whenBodyIsMalformedOrPathTypeIsWrong() throws Exception {
        mockMvc.perform(post("/fake/validate").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/fake/typed/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn405_whenMethodIsNotSupported() throws Exception {
        mockMvc.perform(delete("/fake/conflict")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldHideInternalDetails_onUnexpectedErrors() throws Exception {
        mockMvc.perform(get("/fake/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }

    record FakeBody(@NotBlank String name) {
    }

    @RestController
    static class FakeController {

        @GetMapping("/fake/not-found")
        void notFound() {
            throw new ResourceNotFoundException("thing 1 not found");
        }

        @GetMapping("/fake/conflict")
        void conflict() {
            throw new ConflictException("duplicated");
        }

        @GetMapping("/fake/business")
        void business() {
            throw new BusinessRuleException("rule violated");
        }

        @GetMapping("/fake/remote")
        void remote() {
            throw new RemoteServiceException("service down");
        }

        @GetMapping("/fake/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("SQL constraint uk_email");
        }

        @GetMapping("/fake/optimistic")
        void optimistic() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/fake/denied")
        void denied() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/fake/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/fake/typed/{id}")
        void typed(@PathVariable Long id) {
            throw new IllegalStateException("should not reach here " + id);
        }

        @PostMapping("/fake/validate")
        void validate(@Valid @RequestBody FakeBody body) {
            throw new IllegalStateException("should not reach here " + body);
        }
    }
}
