package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.security.AuthenticationService;
import io.github.susimsek.springdataaotsamples.security.CookieUtils;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.LogoutRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RefreshTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "authentication", description = "Authentication APIs")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticates user and returns JWT access token.")
    @ApiResponse(
            responseCode = "200",
            description = "Token issued",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenDTO.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginRequest request) {
        TokenDTO token = authenticationService.login(request);
        ResponseCookie cookie = CookieUtils.authCookie(token.token(), token.expiresAt());
        ResponseCookie refreshCookie =
                CookieUtils.refreshCookie(token.refreshToken(), token.refreshTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(token);
    }

    @SecurityRequirements
    @Operation(summary = "Register", description = "Creates a new user account.")
    @ApiResponse(
            responseCode = "201",
            description = "User registered",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegistrationDTO.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Username or email already exists",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegistrationDTO> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
    }

    @Operation(
            summary = "Current user",
            description = "Returns details for the authenticated principal.")
    @ApiResponse(
            responseCode = "200",
            description = "Current principal info",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/me")
    public UserDTO me() {
        return authenticationService.getCurrentUser();
    }

    @Operation(summary = "Logout", description = "Clears auth cookie.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            @Nullable @Valid @RequestBody(required = false) LogoutRequest body) {
        String refreshToken = body != null ? body.refreshToken() : null;
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = CookieUtils.getCookieValue(request, SecurityUtils.REFRESH_COOKIE);
        }
        authenticationService.logout(refreshToken);
        ResponseCookie clearCookie = CookieUtils.clearAuthCookie();
        ResponseCookie clearRefresh = CookieUtils.clearRefreshCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .build();
    }

    @SecurityRequirements
    @Operation(
            summary = "Refresh access token",
            description = "Rotates refresh token and issues a new access token.")
    @ApiResponse(
            responseCode = "200",
            description = "New tokens issued",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenDTO.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDTO> refresh(
            HttpServletRequest request,
            @Nullable @Valid @RequestBody(required = false) RefreshTokenRequest body) {
        String refreshToken = body != null ? body.refreshToken() : null;
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = CookieUtils.getCookieValue(request, SecurityUtils.REFRESH_COOKIE);
        }
        TokenDTO token = authenticationService.refresh(refreshToken);
        ResponseCookie accessCookie = CookieUtils.authCookie(token.token(), token.expiresAt());
        ResponseCookie refreshCookie =
                CookieUtils.refreshCookie(token.refreshToken(), token.refreshTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(token);
    }
}
