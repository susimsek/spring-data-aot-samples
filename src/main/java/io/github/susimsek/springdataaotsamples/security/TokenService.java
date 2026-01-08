package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final ApplicationProperties applicationProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenDTO generateToken(Authentication authentication, boolean rememberMe) {
        UserPrincipal principal = extractPrincipal(authentication);
        return issueTokens(principal, rememberMe);
    }

    @Transactional
    public TokenDTO refresh(@Nullable String refreshTokenValue) {
        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new InvalidBearerTokenException("Refresh token is missing");
        }
        String tokenHash = HashingUtils.sha256Hex(refreshTokenValue);
        RefreshToken existing =
                refreshTokenRepository
                        .findByTokenAndRevokedFalse(tokenHash)
                        .orElseThrow(
                                () -> new InvalidBearerTokenException("Invalid refresh token"));
        refreshTokenService.revoke(existing);

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidBearerTokenException("Refresh token expired");
        }

        User user =
                userRepository
                        .findOneWithAuthoritiesById(existing.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        boolean rememberMe = existing.isRememberMe();
        UserPrincipal principal = UserPrincipal.from(user);
        return issueTokens(principal, rememberMe);
    }

    private String encodeAccessToken(
            UserPrincipal principal,
            Set<@Nullable String> authorities,
            Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(jwtProperties().getIssuer())
                        .audience(jwtProperties().getAudience())
                        .subject(principal.getUsername())
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .claim(SecurityUtils.AUTHORITIES_CLAIM, authorities)
                        .claim(SecurityUtils.USER_ID_CLAIM, principal.id())
                        .build();

        var headers = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        var parameters = JwtEncoderParameters.from(headers, claims);
        return jwtEncoder.encode(parameters).getTokenValue();
    }

    private RefreshToken createRefreshToken(
            UserPrincipal principal, Instant issuedAt, boolean rememberMe) {
        Long userId = principal.id();
        var refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setIssuedAt(issuedAt);
        var ttl =
                rememberMe
                        ? jwtProperties().getRefreshTokenTtlForRememberMe()
                        : jwtProperties().getRefreshTokenTtl();
        refreshToken.setExpiresAt(issuedAt.plus(ttl));
        refreshToken.setRememberMe(rememberMe);
        String rawToken = RandomUtils.hexRefreshToken();
        refreshToken.setToken(HashingUtils.sha256Hex(rawToken));
        refreshToken.setRawToken(rawToken);
        return refreshTokenRepository.save(refreshToken);
    }

    private TokenDTO issueTokens(UserPrincipal principal, boolean rememberMe) {
        Set<@Nullable String> authorities =
                principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(jwtProperties().getAccessTokenTtl());
        String accessToken = encodeAccessToken(principal, authorities, now, accessExpiresAt);

        RefreshToken refreshToken = createRefreshToken(principal, now, rememberMe);
        return new TokenDTO(
                accessToken,
                "Bearer",
                accessExpiresAt,
                refreshToken.getRawToken(),
                refreshToken.getExpiresAt(),
                principal.getUsername(),
                authorities);
    }

    private UserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new BadCredentialsException("Unsupported authentication principal");
    }

    private ApplicationProperties.Jwt jwtProperties() {
        return applicationProperties.getSecurity().getJwt();
    }
}
