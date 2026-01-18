package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.service.command.UserCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.ChangePasswordRequest;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenDTO login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(), request.password()));

        return tokenService.generateToken(authentication, request.rememberMe());
    }

    public TokenDTO refresh(@Nullable String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    public RegistrationDTO register(RegisterRequest request) {
        return userCommandService.register(request);
    }

    public UserDTO getCurrentUser() {
        var login =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userQueryService.getUserWithAuthoritiesByUsername(login);
    }

    public void logout(@Nullable String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        String tokenHash = HashingUtils.sha256Hex(refreshToken);
        var tokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(tokenHash);
        tokenOpt.ifPresent(
                token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId =
                SecurityUtils.getCurrentUserId()
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userCommandService.changePassword(userId, request);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
