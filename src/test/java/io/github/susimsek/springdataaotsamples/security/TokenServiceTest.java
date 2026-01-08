package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.ApplicationDefaults;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private JwtEncoder jwtEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;

    @Spy private ApplicationProperties applicationProperties = buildProps();

    @InjectMocks private TokenService tokenService;

    @Test
    void generateTokenShouldEncodeAndPersistRefresh() {
        Authentication auth = mock(Authentication.class);
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setEnabled(true);
        user.setAuthorities(Set.of());
        UserPrincipal principal = UserPrincipal.from(user);
        when(auth.getPrincipal()).thenReturn(principal);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(
                        Jwt.withTokenValue("access")
                                .header("alg", "HS256")
                                .claim("sub", "alice")
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(60))
                                .build());
        RefreshToken saved = new RefreshToken();
        saved.setUserId(10L);
        saved.setIssuedAt(Instant.now());
        saved.setExpiresAt(Instant.now().plusSeconds(120));
        saved.setRememberMe(false);
        saved.setToken("hash");
        saved.setRawToken("raw");
        when(refreshTokenRepository.save(any())).thenReturn(saved);

        try (MockedStatic<RandomUtils> random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(RandomUtils::hexRefreshToken).thenReturn("raw");

            TokenDTO dto = tokenService.generateToken(auth, false);

            assertThat(dto.token()).isEqualTo("access");
            assertThat(dto.refreshToken()).isEqualTo("raw");
        }
    }

    @Test
    void generateTokenShouldThrowWhenPrincipalUnsupported() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("string-user");

        assertThatThrownBy(() -> tokenService.generateToken(auth, false))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshShouldRejectMissingOrInvalidToken() {
        assertThatThrownBy(() -> tokenService.refresh(" "))
                .isInstanceOf(InvalidBearerTokenException.class);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("raw")))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> tokenService.refresh("raw"))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void refreshShouldRejectExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setToken(HashingUtils.sha256Hex("raw"));
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("raw")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.refresh("raw"))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void refreshShouldRejectDisabledUser() {
        RefreshToken token = new RefreshToken();
        token.setUserId(1L);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("raw")))
                .thenReturn(Optional.of(token));
        User user = new User();
        user.setEnabled(false);
        when(userRepository.findOneWithAuthoritiesById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenService.refresh("raw")).isInstanceOf(DisabledException.class);
    }

    @Test
    void refreshShouldIssueNewTokensAndRevokeOld() {
        RefreshToken token = new RefreshToken();
        token.setUserId(1L);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRememberMe(true);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("raw")))
                .thenReturn(Optional.of(token));
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEnabled(true);
        user.setAuthorities(Set.of());
        when(userRepository.findOneWithAuthoritiesById(1L)).thenReturn(Optional.of(user));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(
                        Jwt.withTokenValue("access2")
                                .header("alg", "HS256")
                                .claim("sub", "alice")
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(60))
                                .build());
        RefreshToken saved = new RefreshToken();
        saved.setUserId(1L);
        saved.setExpiresAt(Instant.now().plusSeconds(120));
        saved.setIssuedAt(Instant.now());
        saved.setRememberMe(true);
        saved.setToken("hash2");
        saved.setRawToken("raw2");
        when(refreshTokenRepository.save(any())).thenReturn(saved);

        try (MockedStatic<RandomUtils> random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(RandomUtils::hexRefreshToken).thenReturn("raw2");

            TokenDTO dto = tokenService.refresh("raw");

            assertThat(dto.token()).isEqualTo("access2");
            assertThat(dto.refreshToken()).isEqualTo("raw2");
            verify(refreshTokenService).revoke(token);
        }
    }

    private ApplicationProperties buildProps() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSecurity().getJwt().setIssuer("issuer");
        properties.getSecurity().getJwt().setAudience(ApplicationDefaults.Security.Jwt.audience);
        properties.getSecurity().getJwt().setAccessTokenTtl(Duration.ofSeconds(60));
        properties.getSecurity().getJwt().setRefreshTokenTtl(Duration.ofSeconds(120));
        properties.getSecurity().getJwt().setRefreshTokenTtlForRememberMe(Duration.ofSeconds(180));
        return properties;
    }
}
