package com.acme.employeemanagement.security;

import org.springframework.security.core.Authentication;
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
 * Issues the bearer tokens the Angular client sends back on every request.
 *
 * <p>Tokens are signed with a shared secret (HS256) and carry the caller's roles,
 * which keeps the API stateless: no server-side session store to scale or
 * invalidate. The trade-off is that a token cannot be revoked before it expires,
 * so the lifetime is deliberately short.
 */
@Service
public class JwtTokenService {

    private static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            SecurityProperties securityProperties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    public IssuedToken issue(Authentication authentication) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(securityProperties.getJwt().getTtl());

        // Only ROLE_ authorities become claims. Spring Security also grants
        // authentication-factor authorities such as FACTOR_PASSWORD, which
        // describe how the user signed in, not what they may do.
        List<String> roles = Roles.of(authentication);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim(ROLES_CLAIM, roles)
                .build();

        // The algorithm has to be stated explicitly: the encoder defaults to
        // RS256 and would fail to match the symmetric signing key.
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                ))
                .getTokenValue();

        return new IssuedToken(token, expiresAt, roles);
    }

    public record IssuedToken(
            String token,
            Instant expiresAt,
            List<String> roles
    ) {
    }
}
