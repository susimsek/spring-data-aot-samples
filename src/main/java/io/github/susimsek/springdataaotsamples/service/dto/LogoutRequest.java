package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Logout payload")
public record LogoutRequest(
        @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @Size(min = 20, max = 512)
        String refreshToken
) {
}
