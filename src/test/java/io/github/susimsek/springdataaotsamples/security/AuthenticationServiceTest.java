package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.UserMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void loginShouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest("alice", "pwd", true);
        Authentication auth = mock(Authentication.class);
        TokenDTO token =
                new TokenDTO(
                        "jwt",
                        "Bearer",
                        Instant.now().plusSeconds(60),
                        "refresh",
                        Instant.now().plusSeconds(120),
                        "alice",
                        Set.of(AuthoritiesConstants.USER));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(tokenService.generateToken(auth, true)).thenReturn(token);

        TokenDTO result = authenticationService.login(request);

        assertThat(result).isEqualTo(token);
        verify(authenticationManager)
                .authenticate(new UsernamePasswordAuthenticationToken("alice", "pwd"));
    }

    @Test
    void getCurrentUserShouldLoadUserAndMap() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        UserDTO dto =
                new UserDTO(1L, "alice", "alice@example.com", Set.of(AuthoritiesConstants.USER));
        when(userRepository.findOneWithAuthoritiesByUsername("alice"))
                .thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            UserDTO result = authenticationService.getCurrentUser();

            assertThat(result).isEqualTo(dto);
        }
    }

    @Test
    void getCurrentUserShouldThrowWhenMissing() {
        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Test
    void logoutShouldIgnoreBlankToken() {
        authenticationService.logout("  ");
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutShouldHashAndRevokeTokenWhenExists() {
        RefreshToken token = new RefreshToken();
        token.setToken("hashed");
        token.setRevoked(false);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("raw")))
                .thenReturn(Optional.of(token));

        authenticationService.logout("raw");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void refreshShouldDelegateToTokenService() {
        TokenDTO dto =
                new TokenDTO(
                        "jwt",
                        "Bearer",
                        Instant.now(),
                        "refresh",
                        Instant.now(),
                        "alice",
                        Set.of(AuthoritiesConstants.USER));
        when(tokenService.refresh("refresh-token")).thenReturn(dto);

        TokenDTO result = authenticationService.refresh("refresh-token");

        assertThat(result).isEqualTo(dto);
        verify(tokenService).refresh("refresh-token");
    }
}
