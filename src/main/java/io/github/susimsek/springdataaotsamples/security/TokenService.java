package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.config.JwtProperties;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public TokenDTO generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenTtl());

        String username = authentication.getName();
        Long userId = null;
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.id();
            username = principal.getUsername();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(username)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SecurityUtils.AUTHORITIES_CLAIM, authorities)
                .claim(SecurityUtils.USER_ID_CLAIM, userId)
                .build();

        var headers = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        var parameters = JwtEncoderParameters.from(headers, claims);
        var jwt = jwtEncoder.encode(parameters);

        return new TokenDTO(
                jwt.getTokenValue(),
                "Bearer",
                expiresAt,
                username,
                authorities
        );
    }
}
