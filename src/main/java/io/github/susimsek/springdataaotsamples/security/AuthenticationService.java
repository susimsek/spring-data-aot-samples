package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

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

    public UserDTO getCurrentUser() {
        var login =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var user =
                userRepository
                        .findOneWithAuthoritiesByUsername(login)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userMapper.toDto(user);
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
}
