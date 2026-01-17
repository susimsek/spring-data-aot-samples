package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration result")
public record RegistrationDTO(
        @Schema(description = "Registered user identifier", example = "42") Long id,
        @Schema(description = "Registered username", example = "newuser") String username) {}
