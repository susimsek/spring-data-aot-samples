package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Change password payload")
public record ChangePasswordRequest(
        @Schema(description = "Current password", example = "admin") @NotBlank
                String currentPassword,
        @Schema(description = "New password", example = "Change-me1!")
                @NotBlank
                @Size(min = 8, max = 64)
                @Password
                String newPassword) {}
