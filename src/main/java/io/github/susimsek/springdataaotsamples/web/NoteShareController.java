package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/notes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "note-share", description = "Share links for notes")
public class NoteShareController {

    private final NoteShareService noteShareService;

    @Operation(
            summary = "Create share link",
            description = "Creates a time-bound share token for a note. Requires ownership.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Share token created",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NoteShareDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Note not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping(value = "/{id}/share", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteShareDTO create(@Parameter(description = "Note identifier") @PathVariable Long id,
                                    @Valid @RequestBody CreateShareTokenRequest request) {
        return noteShareService.createForCurrentUser(id, request);
    }

    @Operation(
            summary = "List share links",
            description = "Lists all share tokens for a note. Requires ownership.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Share tokens fetched",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NoteShareDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Note not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/{id}/share")
    public Page<NoteShareDTO> list(@Parameter(description = "Note identifier") @PathVariable Long id,
                                   @ParameterObject Pageable pageable) {
        return noteShareService.listForCurrentUser(id, pageable);
    }

    @Operation(
            summary = "List all share links of current user",
            description = "Lists all share tokens created by the current user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Share tokens fetched",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NoteShareDTO.class)))
            }
    )
    @GetMapping("/share")
    public Page<NoteShareDTO> listMine(@ParameterObject Pageable pageable) {
        return noteShareService.listAllForCurrentUser(pageable);
    }

    @Operation(
            summary = "Revoke share link",
            description = "Revokes an existing share token. Requires ownership.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Share token revoked"),
                    @ApiResponse(responseCode = "404", description = "Share token not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @DeleteMapping("/share/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Parameter(description = "Share token identifier") @PathVariable Long tokenId) {
        noteShareService.revokeForCurrentUser(tokenId);
    }
}
