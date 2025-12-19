package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.HexColor;
import io.github.susimsek.springdataaotsamples.service.validation.constraints.TagValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Payload to create a note")
public record NoteCreateRequest(
        @Schema(
                        description = "Note title",
                        minLength = 3,
                        maxLength = 255,
                        example = "My first note")
                @NotBlank
                @Size(min = 3, max = 255)
                String title,
        @Schema(
                        description = "Note content",
                        minLength = 10,
                        maxLength = 1024,
                        example = "Hello auditing world")
                @NotBlank
                @Size(min = 10, max = 1024)
                String content,
        @Schema(description = "Whether the note is pinned", example = "false") @NotNull
                Boolean pinned,
        @Schema(description = "Optional color hex (e.g. #2563eb)", example = "#2563eb") @HexColor
                String color,
        @Schema(description = "Tags attached to the note", example = "[\"audit\",\"liquibase\"]")
                @Size(max = 5)
                Set<@NotBlank @Size(min = 1, max = 30) @TagValue String> tags) {}
