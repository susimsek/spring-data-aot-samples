package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change note owner (admin only)")
public record OwnerChangeRequest(
        @Schema(description = "New owner username", example = "alice")
                @NotBlank @Username
                @Size(min = 3, max = 100) String owner) {}
