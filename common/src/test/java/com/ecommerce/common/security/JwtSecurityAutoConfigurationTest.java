package com.ecommerce.common.security;

import com.ecommerce.common.config.JwtSecurityAutoConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa a auto-configuracao com o {@link WebApplicationContextRunner}: sobe um contexto
 * minimo apenas com as classes necessarias, o que e muito mais rapido que um @SpringBootTest.
 */
class JwtSecurityAutoConfigurationTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters!!";
    private static final String ISSUER = "test-issuer";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, JwtSecurityAutoConfiguration.class));

    @Test
    void shouldDecodeValidTokenAndMapRolesToAuthorities() {
        runner.withPropertyValues("security.jwt.secret=" + SECRET, "security.jwt.issuer=" + ISSUER)
                .run(context -> {
                    JwtDecoder decoder = context.getBean(JwtDecoder.class);
                    Jwt jwt = decoder.decode(token(SECRET, ISSUER, Instant.now().plusSeconds(60)));

                    var authentication = context.getBean(JwtAuthenticationConverter.class).convert(jwt);

                    assertThat(authentication.getName()).isEqualTo("alice");
                    assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_ADMIN");
                    assertThat(context).hasSingleBean(StatelessJwtSecurity.class);
                });
    }

    @Test
    void shouldRejectTokenFromAnotherIssuer() {
        runner.withPropertyValues("security.jwt.secret=" + SECRET, "security.jwt.issuer=" + ISSUER)
                .run(context -> {
                    JwtDecoder decoder = context.getBean(JwtDecoder.class);
                    String foreign = token(SECRET, "someone-else", Instant.now().plusSeconds(60));
                    assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtValidationException.class);
                });
    }

    @Test
    void shouldRejectExpiredToken() {
        runner.withPropertyValues("security.jwt.secret=" + SECRET, "security.jwt.issuer=" + ISSUER)
                .run(context -> {
                    String expired = token(SECRET, ISSUER, Instant.now().minusSeconds(3600));
                    assertThatThrownBy(() -> context.getBean(JwtDecoder.class).decode(expired))
                            .isInstanceOf(JwtValidationException.class);
                });
    }

    @Test
    void shouldFailFast_whenSecretIsTooShort() {
        runner.withPropertyValues("security.jwt.secret=short", "security.jwt.issuer=" + ISSUER)
                .run(context -> assertThat(context).hasFailed());
    }

    private static String token(String secret, String issuer, Instant expiresAt) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("alice")
                .issuedAt(expiresAt.minusSeconds(120))
                .expiresAt(expiresAt)
                .claim(Roles.CLAIM, List.of(Roles.ADMIN))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
