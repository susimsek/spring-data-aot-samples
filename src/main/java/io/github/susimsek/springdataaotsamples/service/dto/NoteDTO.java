package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(description = "Note view model")
public record NoteDTO(
        @Schema(description = "Identifier", example = "1") Long id,
        @Schema(description = "Note title", example = "My first note") String title,
        @Schema(description = "Note content", example = "Hello auditing world") String content,
        @Schema(description = "Pinned flag to keep note on top", example = "false") boolean pinned,
        @Schema(description = "Optional color in hex format", example = "#2563eb") String color,
        @Schema(description = "Owner of the note", example = "alice") String owner,
        @Schema(description = "Tags attached to the note", example = "[\"audit\",\"liquibase\"]")
                Set<String> tags,
        @Schema(description = "Optimistic lock version", example = "1") Long version,
        @Schema(description = "Created by auditor", example = "alice") String createdBy,
        @Schema(description = "Created timestamp", example = "2024-01-01T10:15:30Z")
                Instant createdDate,
        @Schema(description = "Last modified by auditor", example = "bob") String lastModifiedBy,
        @Schema(description = "Last modified timestamp", example = "2024-01-02T10:15:30Z")
                Instant lastModifiedDate,
        @Schema(description = "Soft delete flag", example = "false") boolean deleted,
        @Schema(description = "Deleted by", example = "system") String deletedBy,
        @Schema(description = "Deleted timestamp", example = "2024-01-03T10:15:30Z")
                Instant deletedDate) {}
