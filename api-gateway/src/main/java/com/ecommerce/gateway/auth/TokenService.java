package com.ecommerce.gateway.auth;

import com.ecommerce.gateway.auth.AuthDtos.TokenResponse;
import com.ecommerce.gateway.config.GatewaySecurityProperties;
import com.ecommerce.gateway.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Emite tokens JWT para usuarios autenticados.
 *
 * <p>O token contem apenas o necessario (sub, roles, iss, iat, exp). Nunca coloque dados sensiveis no JWT:
 * ele e apenas assinado, NAO criptografado, e qualquer pessoa consegue ler o conteudo (base64).</p>
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final GatewaySecurityProperties properties;
    private final Clock clock;

    /** Com dois construtores, {@code @Autowired} indica qual o Spring deve usar. O outro existe para testes. */
    @Autowired
    public TokenService(JwtEncoder encoder, GatewaySecurityProperties properties) {
        this(encoder, properties, Clock.systemUTC());
    }

    TokenService(JwtEncoder encoder, GatewaySecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenResponse issue(Authentication authentication) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().expiration());
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SecurityConfig.ROLES_CLAIM, roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", properties.jwt().expiration().toSeconds(), roles);
    }
}
