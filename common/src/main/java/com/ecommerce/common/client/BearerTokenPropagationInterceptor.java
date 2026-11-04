package com.ecommerce.common.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;

/**
 * Repassa o token JWT do usuario atual nas chamadas REST para outros servicos.
 *
 * <p>Como cada servico valida o token (defesa em profundidade), o Order Service precisa
 * reenviar o mesmo token ao chamar Customer, Product e Inventory. Isso tambem preserva a
 * identidade do usuario em toda a cadeia de chamadas.</p>
 */
public class BearerTokenPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken
                && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            request.getHeaders().setBearerAuth(jwtToken.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
