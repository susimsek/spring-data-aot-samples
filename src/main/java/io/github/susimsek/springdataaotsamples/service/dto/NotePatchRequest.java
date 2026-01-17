package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.HexColor;
import io.github.susimsek.springdataaotsamples.service.validation.constraints.TagValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.jspecify.annotations.Nullable;

@Schema(description = "Payload to partially update a note")
public record NotePatchRequest(
        @Schema(description = "Note title", example = "Updated note title")
                @Nullable
                @Size(min = 3, max = 255)
                String title,
        @Schema(description = "Note content", example = "Patched content")
                @Nullable
                @Size(min = 10, max = 1024)
                String content,
        @Schema(description = "Pinned flag", example = "true") @Nullable Boolean pinned,
        @Schema(description = "Optional color hex (e.g. #2563eb)", example = "#2563eb")
                @Nullable
                @HexColor
                String color,
        @Schema(description = "Tags attached to the note", example = "[\"audit\",\"liquibase\"]")
                @Nullable
                @Size(max = 5)
                Set<@NotBlank @Size(min = 1, max = 30) @TagValue String> tags) {}
