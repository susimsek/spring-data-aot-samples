package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Share token view model")
public record NoteShareDTO(
    @Schema(description = "Identifier of the share token", example = "1") Long id,
    @Schema(description = "Raw share token to be used by guests", example = "3b2c...") String token,
    @Schema(description = "Note identifier the token is scoped to", example = "42") Long noteId,
    @Schema(description = "Permission granted by the token", example = "READ")
        SharePermission permission,
    @Schema(description = "Expiry timestamp", example = "2024-12-31T23:59:59Z") Instant expiresAt,
    @Schema(description = "Whether the token is one-time use", example = "false") boolean oneTime,
    @Schema(description = "Whether the token is revoked", example = "false") boolean revoked,
    @Schema(description = "How many times the link has been used", example = "1") int useCount,
    @Schema(description = "Creation timestamp of the share link", example = "2024-12-12T18:58:42Z")
        Instant createdDate,
    @Schema(description = "Last use or update timestamp", example = "2024-12-12T19:10:00Z")
        Instant lastUsedAt,
    @Schema(
            description = "Whether the link is expired at the time of the response",
            example = "true")
        boolean expired,
    @Schema(description = "Title of the note", example = "Welcome note") String noteTitle,
    @Schema(description = "Owner of the note", example = "system") String noteOwner) {}
