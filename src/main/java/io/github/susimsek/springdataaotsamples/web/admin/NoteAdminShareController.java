package io.github.susimsek.springdataaotsamples.web.admin;

import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/notes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "admin-note-share", description = "Admin share links for notes")
public class NoteAdminShareController {

    private final NoteShareService noteShareService;

    @Operation(
            summary = "Create share link (admin)",
            description = "Creates a share token for a note without requiring ownership.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Share token created",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = NoteShareDTO.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Note not found",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping(value = "/{id}/share", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteShareDTO create(
            @PathVariable Long id, @Valid @RequestBody CreateShareTokenRequest request) {
        return noteShareService.create(id, request);
    }

    @Operation(
            summary = "List share links (admin)",
            description = "Lists all share tokens for a note.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Share tokens fetched",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = NoteShareDTO.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Note not found",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @GetMapping("/{id}/share")
    public Page<NoteShareDTO> list(
            @PathVariable Long id,
            @ParameterObject Pageable pageable,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdFrom", required = false) Instant createdFrom,
            @RequestParam(name = "createdTo", required = false) Instant createdTo) {
        return noteShareService.listForAdmin(id, pageable, q, status, createdFrom, createdTo);
    }

    @Operation(
            summary = "List all share links (admin)",
            description = "Lists all share tokens across notes.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Share tokens fetched",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = NoteShareDTO.class)))
            })
    @GetMapping("/share")
    public Page<NoteShareDTO> listAll(
            @ParameterObject Pageable pageable,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdFrom", required = false) Instant createdFrom,
            @RequestParam(name = "createdTo", required = false) Instant createdTo) {
        return noteShareService.listAllForAdmin(pageable, q, status, createdFrom, createdTo);
    }

    @Operation(
            summary = "Revoke share link (admin)",
            description = "Revokes an existing share token without ownership requirement.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Share token revoked"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Share token not found",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @DeleteMapping("/share/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long tokenId) {
        noteShareService.revoke(tokenId);
    }
}
