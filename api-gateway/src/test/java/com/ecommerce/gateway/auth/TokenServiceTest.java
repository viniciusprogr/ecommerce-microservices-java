package com.ecommerce.gateway.auth;

import com.ecommerce.gateway.config.GatewaySecurityProperties;
import com.ecommerce.gateway.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private static final GatewaySecurityProperties PROPERTIES = new GatewaySecurityProperties(
            new GatewaySecurityProperties.Jwt("unit-test-secret-with-more-than-32-chars!", "test-issuer", Duration.ofMinutes(30)),
            List.of(new GatewaySecurityProperties.UserAccount("admin", "pw", List.of("ADMIN"))));

    @Test
    void shouldIssueSignedTokenWithRolesAndExpiration() {
        var config = new SecurityConfig();
        Instant now = Instant.now();
        var service = new TokenService(config.jwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));
        var authentication = UsernamePasswordAuthenticationToken.authenticated("alice", null,
                AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN"));

        var response = service.issue(authentication);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.roles()).containsExactly("ADMIN", "USER");

        Jwt jwt = config.reactiveJwtDecoder(PROPERTIES).decode(response.accessToken()).block();
        assertThat(jwt).isNotNull();
        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "USER");
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }
}
