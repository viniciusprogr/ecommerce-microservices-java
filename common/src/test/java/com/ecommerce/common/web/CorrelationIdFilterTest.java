package com.ecommerce.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldReuseIncomingCorrelationId_whenValueIsSafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInsideChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenInsideChain.set(MDC.get(CorrelationId.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(seenInsideChain.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).as("MDC must be cleaned after the request").isNull();
    }

    @Test
    void shouldGenerateNewCorrelationId_whenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationId.HEADER)).hasSize(36);
    }

    @Test
    void shouldRejectUnsafeValues_toPreventLogInjection() {
        assertThat(CorrelationIdFilter.resolve("evil\nFAKE LOG LINE")).isNotEqualTo("evil\nFAKE LOG LINE").hasSize(36);
        assertThat(CorrelationIdFilter.resolve("a".repeat(65))).hasSize(36);
        assertThat(CorrelationIdFilter.resolve(" ")).hasSize(36);
        assertThat(CorrelationIdFilter.resolve(null)).hasSize(36);
    }
}
