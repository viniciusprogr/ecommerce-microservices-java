package com.ecommerce.common.config;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuracao web comum.
 *
 * <p>Registrada em {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * Basta adicionar o modulo {@code common} como dependencia para que o servico ganhe o tratamento
 * global de erros e o Correlation ID, sem precisar de {@code @Import} ou component scan.
 * E o mesmo mecanismo usado pelos "starters" do proprio Spring Boot.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * O filtro roda antes de todos os outros (inclusive Spring Security), para que ate
     * erros de autenticacao tenham Correlation ID nos logs e na resposta.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
