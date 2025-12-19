package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "Authenticated principal details")
public record UserDTO(
    @Schema(description = "User identifier") Long id,
    @Schema(description = "Authenticated username", example = "admin") String username,
    @Schema(description = "Granted authorities", example = "[\"ROLE_USER\"]")
        Set<String> authorities) {}
