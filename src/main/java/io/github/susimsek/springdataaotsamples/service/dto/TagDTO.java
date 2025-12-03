package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tag view model")
public record TagDTO(
        @Schema(description = "Identifier", example = "1")
        Long id,
        @Schema(description = "Tag name", example = "audit")
        String name
) {
}
