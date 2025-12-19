package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Login payload")
public record LoginRequest(
        @Schema(description = "Username", example = "admin") @NotBlank String username,
        @Schema(description = "Password", example = "admin") @NotBlank String password,
        @Schema(description = "Remember me", example = "true") @NotNull Boolean rememberMe) {}
