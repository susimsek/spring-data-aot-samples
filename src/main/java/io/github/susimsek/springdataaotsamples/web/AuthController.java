package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RefreshTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.LogoutRequest;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.security.AuthService;
import io.github.susimsek.springdataaotsamples.security.CookieUtils;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticates user and returns JWT access token.")
    @ApiResponse(responseCode = "200", description = "Token issued",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginRequest request) {
        TokenDTO token = authService.login(request);
        ResponseCookie cookie = CookieUtils.authCookie(token.token(), token.expiresAt());
        ResponseCookie refreshCookie = CookieUtils.refreshCookie(token.refreshToken(), token.refreshTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(token);
    }

    @Operation(summary = "Current user", description = "Returns details for the authenticated principal.")
    @ApiResponse(responseCode = "200", description = "Current principal info",
            content = @Content(schema = @Schema(implementation = UserDTO.class)))
    @GetMapping("/me")
    public UserDTO me() {
        return authService.getCurrentUser();
    }

    @Operation(summary = "Logout", description = "Clears auth cookie.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            @RequestBody(required = false) LogoutRequest body
    ) {
        String refreshToken = body.refreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = CookieUtils.getCookieValue(request, SecurityUtils.REFRESH_COOKIE);
        }
        authService.logout(refreshToken);
        ResponseCookie clearCookie = CookieUtils.clearAuthCookie();
        ResponseCookie clearRefresh = CookieUtils.clearRefreshCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .build();
    }

    @SecurityRequirements
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new access token.")
    @ApiResponse(responseCode = "200", description = "New tokens issued",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @PostMapping("/refresh")
    public ResponseEntity<TokenDTO> refresh(
            HttpServletRequest request,
            @Valid @RequestBody(required = false) RefreshTokenRequest body
    ) {
        String refreshToken = body.refreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = CookieUtils.getCookieValue(request, SecurityUtils.REFRESH_COOKIE);
        }
        TokenDTO token = authService.refresh(refreshToken);
        ResponseCookie accessCookie = CookieUtils.authCookie(token.token(), token.expiresAt());
        ResponseCookie refreshCookie = CookieUtils.refreshCookie(token.refreshToken(), token.refreshTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(token);
    }
}
