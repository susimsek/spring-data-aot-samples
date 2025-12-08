package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.NoteService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
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
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.Set;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "notes", description = "Note CRUD APIs")
public class NoteController {

    private final NoteService noteService;

    @Operation(
            summary = "Create note",
            description = "Creates a note and returns the persisted resource with auditing metadata."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Note created",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDTO create(@Valid @RequestBody NoteCreateRequest request) {
        return noteService.create(request);
    }

    @Operation(
            summary = "List notes",
            description = "Returns paged notes."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paged notes",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @GetMapping
    public Page<NoteDTO> findAll(@ParameterObject Pageable pageable,
                                 @RequestParam(value = "q", required = false)
                                 @Parameter(description = "Search in title or content") String q,
                                 @RequestParam(value = "tags", required = false)
                                 @Parameter(description = "Filter by tag names") Set<String> tags,
                                 @RequestParam(value = "color", required = false)
                                 @Parameter(description = "Exact color hex filter") String color,
                                 @RequestParam(value = "pinned", required = false)
                                 @Parameter(description = "Filter by pinned state") Boolean pinned) {
        return noteService.findAllForCurrentUser(pageable, q, tags, color, pinned);
    }

    @Operation(
            summary = "List deleted notes",
            description = "Returns paged soft-deleted notes."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paged deleted notes",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @GetMapping("/deleted")
    public Page<NoteDTO> findDeleted(@ParameterObject Pageable pageable,
                                     @RequestParam(value = "q", required = false)
                                     @Parameter(description = "Search in title or content") String q,
                                     @RequestParam(value = "tags", required = false)
                                     @Parameter(description = "Filter by tag names") Set<String> tags,
                                     @RequestParam(value = "color", required = false)
                                     @Parameter(description = "Exact color hex filter") String color,
                                     @RequestParam(value = "pinned", required = false)
                                     @Parameter(description = "Filter by pinned state") Boolean pinned) {
        return noteService.findDeletedForCurrentUser(pageable, q, tags, color, pinned);
    }

    @Operation(
            summary = "Empty trash",
            description = "Permanently deletes all soft-deleted notes."
    )
    @ApiResponse(responseCode = "204", description = "Trash emptied")
    @DeleteMapping("/deleted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emptyTrash() {
        noteService.emptyTrashForCurrentUser();
    }

    @Operation(
            summary = "Update note",
            description = "Updates title and content of a note."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note updated",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public NoteDTO update(
            @Parameter(description = "Note identifier") @PathVariable Long id,
            @Valid @RequestBody NoteUpdateRequest request
    ) {
        return noteService.updateForCurrentUser(id, request);
    }

    @Operation(
            summary = "Patch note",
            description = "Partially updates a note."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note patched",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    public NoteDTO patch(
            @Parameter(description = "Note identifier") @PathVariable Long id,
            @Valid @RequestBody NotePatchRequest request
    ) {
        return noteService.patchForCurrentUser(id, request);
    }

    @Operation(
            summary = "Delete note",
            description = "Deletes a note by id."
    )
    @ApiResponse(responseCode = "204", description = "Note deleted")
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.deleteForCurrentUser(id);
    }

    @Operation(
            summary = "Bulk actions on notes",
            description = "Performs bulk soft delete, restore, or permanent delete on provided ids."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Bulk action result",
            content = @Content(schema = @Schema(implementation = BulkActionResult.class))
    )
    @ApiResponse(responseCode = "404", description = "Notes not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/bulk")
    public BulkActionResult bulk(@Valid @RequestBody BulkActionRequest request) {
        return noteService.bulkForCurrentUser(request);
    }

    @Operation(
            summary = "Restore note",
            description = "Restores a soft-deleted note."
    )
    @ApiResponse(responseCode = "204", description = "Note restored")
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.restoreForCurrentUser(id);
    }

    @Operation(
            summary = "Delete note permanently",
            description = "Permanently deletes a soft-deleted note."
    )
    @ApiResponse(responseCode = "204", description = "Note permanently deleted")
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.deletePermanentlyForCurrentUser(id);
    }

    @Operation(
            summary = "Get note",
            description = "Fetches a single note by id."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note found",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public NoteDTO findById(@Parameter(description = "Note identifier") @PathVariable Long id) {
        return noteService.findByIdForCurrentUser(id);
    }

    @Operation(
            summary = "List revisions of a note",
            description = "Returns historical revisions for a note including snapshots."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Revisions returned",
            content = @Content(schema = @Schema(implementation = NoteRevisionDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}/revisions")
    public Page<NoteRevisionDTO> findRevisions(@Parameter(description = "Note identifier") @PathVariable Long id,
                                               @ParameterObject Pageable pageable) {
        return noteService.findRevisionsForCurrentUser(id, pageable);
    }

    @Operation(
            summary = "Get a single revision snapshot",
            description = "Fetches the note snapshot at a specific revision."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Revision found",
            content = @Content(schema = @Schema(implementation = NoteRevisionDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Revision not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}/revisions/{revisionId}")
    public NoteRevisionDTO findRevision(@Parameter(description = "Note identifier") @PathVariable Long id,
                                        @Parameter(description = "Revision number") @PathVariable Long revisionId) {
        return noteService.findRevisionForCurrentUser(id, revisionId);
    }

    @Operation(
            summary = "Restore note to a revision",
            description = "Restores the current note to the content of the specified revision."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note restored",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Revision not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/revisions/{revisionId}/restore")
    public NoteDTO restoreRevision(@Parameter(description = "Note identifier") @PathVariable Long id,
                                   @Parameter(description = "Revision number") @PathVariable Long revisionId) {
        return noteService.restoreRevisionForCurrentUser(id, revisionId);
    }
}
