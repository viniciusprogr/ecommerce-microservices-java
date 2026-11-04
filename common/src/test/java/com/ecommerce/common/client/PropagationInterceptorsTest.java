package com.ecommerce.common.client;

import com.ecommerce.common.web.CorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PropagationInterceptorsTest {

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void shouldPropagateBearerTokenAndCorrelationId() {
        Jwt jwt = Jwt.withTokenValue("token-123").header("alg", "HS256").subject("alice").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MDC.put(CorrelationId.MDC_KEY, "corr-1");

        RestClient.Builder builder = RestClient.builder()
                .requestInterceptor(new BearerTokenPropagationInterceptor())
                .requestInterceptor(new CorrelationIdPropagationInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://remote/things"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
                .andExpect(header(CorrelationId.HEADER, "corr-1"))
                .andRespond(withSuccess());

        builder.build().get().uri("http://remote/things").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void shouldNotAddHeaders_whenThereIsNoAuthenticationOrCorrelationId() {
        RestClient.Builder builder = RestClient.builder()
                .requestInterceptor(new BearerTokenPropagationInterceptor())
                .requestInterceptor(new CorrelationIdPropagationInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://remote/things"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(headerDoesNotExist(CorrelationId.HEADER))
                .andRespond(withSuccess());

        builder.build().get().uri("http://remote/things").retrieve().toBodilessEntity();

        server.verify();
    }
}
