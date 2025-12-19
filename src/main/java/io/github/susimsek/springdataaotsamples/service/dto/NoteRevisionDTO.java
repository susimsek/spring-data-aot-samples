package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Revision snapshot of a note")
public record NoteRevisionDTO(
    @Schema(description = "Revision number", example = "3") Long revision,
    @Schema(description = "Revision type", example = "MOD") String revisionType,
    @Schema(description = "Revision timestamp", example = "2024-01-01T10:15:30Z")
        Instant revisionDate,
    @Schema(description = "Auditor captured for the revision", example = "alice") String auditor,
    @Schema(description = "Note state at this revision") NoteDTO note) {}
