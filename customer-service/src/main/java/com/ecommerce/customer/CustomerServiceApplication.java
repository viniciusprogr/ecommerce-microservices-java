package com.ecommerce.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do Customer Service.
 *
 * <p>{@code @SpringBootApplication} combina {@code @Configuration}, {@code @EnableAutoConfiguration}
 * e {@code @ComponentScan} a partir deste pacote.</p>
 */
@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
