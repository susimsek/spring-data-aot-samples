package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Password;
import io.github.susimsek.springdataaotsamples.service.validation.constraints.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Username (used for login)", example = "newuser", maxLength = 100)
                @NotBlank
                @Size(min = 3, max = 100)
                @Username
                String username,
        @Schema(description = "Email address", example = "newuser@example.com", maxLength = 255)
                @NotBlank
                @Size(max = 255)
                @Email
                String email,
        @Schema(description = "Password", example = "change-me", maxLength = 255)
                @NotBlank
                @Size(min = 8, max = 255)
                @Password
                String password) {}
