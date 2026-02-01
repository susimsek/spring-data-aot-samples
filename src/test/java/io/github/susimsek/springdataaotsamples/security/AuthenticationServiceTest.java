package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.service.command.UserCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.ChangePasswordRequest;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.service.query.UserQueryService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
    @Mock private UserCommandService userCommandService;
    @Mock private UserQueryService userQueryService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

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
    void loginShouldSupportRememberMeFalse() {
        LoginRequest request = new LoginRequest("bob", "secret", false);
        Authentication auth = mock(Authentication.class);
        TokenDTO token =
                new TokenDTO(
                        "jwt2",
                        "Bearer",
                        Instant.now().plusSeconds(30),
                        "refresh2",
                        Instant.now().plusSeconds(60),
                        "bob",
                        Set.of(AuthoritiesConstants.USER));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(tokenService.generateToken(auth, false)).thenReturn(token);

        TokenDTO result = authenticationService.login(request);

        assertThat(result).isEqualTo(token);
        verify(tokenService).generateToken(auth, false);
    }

    @Test
    void getCurrentUserShouldLoadUserAndMap() {
        UserDTO dto =
                new UserDTO(1L, "alice", "alice@example.com", Set.of(AuthoritiesConstants.USER));
        when(userQueryService.getUserWithAuthoritiesByUsername("alice")).thenReturn(dto);

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
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "   "})
    void logoutShouldIgnoreInvalidTokens(String token) {
        authenticationService.logout(token);
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
    void logoutShouldDoNothingWhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(HashingUtils.sha256Hex("unknown")))
                .thenReturn(Optional.empty());

        authenticationService.logout("unknown");

        verify(refreshTokenRepository, never()).save(any());
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

    @Test
    void refreshShouldHandleNullToken() {
        TokenDTO dto =
                new TokenDTO(
                        "jwt",
                        "Bearer",
                        Instant.now(),
                        "refresh",
                        Instant.now(),
                        "alice",
                        Set.of(AuthoritiesConstants.USER));
        when(tokenService.refresh(null)).thenReturn(dto);

        TokenDTO result = authenticationService.refresh(null);

        assertThat(result).isEqualTo(dto);
        verify(tokenService).refresh(null);
    }

    @Test
    void registerShouldDelegateToUserCommandService() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password");
        RegistrationDTO registration = new RegistrationDTO(1L, "newuser");
        when(userCommandService.register(request)).thenReturn(registration);

        RegistrationDTO result = authenticationService.register(request);

        assertThat(result).isEqualTo(registration);
        verify(userCommandService).register(request);
    }

    @Test
    void changePasswordShouldDelegateAndRevokeTokens() {
        ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(42L));

            authenticationService.changePassword(request);

            verify(userCommandService).changePassword(42L, request);
            verify(refreshTokenRepository).revokeAllByUserId(42L);
        }
    }

    @Test
    void changePasswordShouldThrowWhenUserNotAuthenticated() {
        ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.changePassword(request))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userCommandService, never()).changePassword(any(), any());
            verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        }
    }
}
