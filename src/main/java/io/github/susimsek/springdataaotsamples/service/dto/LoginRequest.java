package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Login payload")
public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        @NotBlank
        @Username
        @Size(min = 3, max = 100)
        String username,

        @Schema(description = "Password", example = "admin")
        @NotBlank
        @Size(min = 4, max = 255)
        String password,

        @Schema(description = "Remember me", example = "true")
        @NotNull
        Boolean rememberMe
) {
}
