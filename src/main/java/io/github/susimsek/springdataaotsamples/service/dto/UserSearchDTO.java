package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User search result")
public record UserSearchDTO(
        @Schema(description = "User id", example = "1") Long id,
        @Schema(description = "Username", example = "admin") String username,
        @Schema(description = "Account enabled", example = "true") boolean enabled) {}
