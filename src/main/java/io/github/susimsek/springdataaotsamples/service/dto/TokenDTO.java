package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "JWT token response")
public record TokenDTO(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,
        @Schema(description = "Token type", example = "Bearer")
        String tokenType,
        @Schema(description = "Expiration time (UTC)")
        Instant expiresAt,
        @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,
        @Schema(description = "Refresh token expiration time (UTC)")
        Instant refreshTokenExpiresAt,
        @Schema(description = "Authenticated username", example = "admin")
        String username,
        @Schema(description = "Granted authorities", example = "[\"ROLE_ADMIN\",\"ROLE_USER\"]")
        Set<String> authorities
) {
}
