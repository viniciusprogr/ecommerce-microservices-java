package com.ecommerce.order.config;

import com.ecommerce.common.client.BearerTokenPropagationInterceptor;
import com.ecommerce.common.client.CorrelationIdPropagationInterceptor;
import com.ecommerce.order.client.CustomerClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.ProductClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Cria os clientes HTTP dos outros servicos.
 *
 * <p>O {@link RestClient.Builder} fornecido pelo Spring Boot ja vem com os conversores JSON configurados.
 * Ele e "prototype": cada parametro injetado e uma instancia nova, entao configurar um nao afeta os outros.</p>
 */
@Configuration
public class RestClientConfig {

    @Bean
    public CustomerClient customerClient(RestClient.Builder builder, ServicesProperties properties) {
        return new CustomerClient(build(builder, properties.customerUrl(), properties));
    }

    @Bean
    public ProductClient productClient(RestClient.Builder builder, ServicesProperties properties) {
        return new ProductClient(build(builder, properties.productUrl(), properties));
    }

    @Bean
    public InventoryClient inventoryClient(RestClient.Builder builder, ServicesProperties properties,
                                           ObjectMapper objectMapper) {
        return new InventoryClient(build(builder, properties.inventoryUrl(), properties), objectMapper);
    }

    private static RestClient build(RestClient.Builder builder, String baseUrl, ServicesProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new BearerTokenPropagationInterceptor())
                .requestInterceptor(new CorrelationIdPropagationInterceptor())
                .build();
    }
}
