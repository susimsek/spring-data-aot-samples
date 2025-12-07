package io.github.susimsek.springdataaotsamples.web.admin;

import io.github.susimsek.springdataaotsamples.service.NoteService;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/notes")
@RequiredArgsConstructor
@Tag(name = "admin-notes", description = "Admin-only note APIs")
public class AdminNoteController {

    private final NoteService noteService;

    @Operation(
            summary = "Create note (admin)",
            description = "Creates a note."
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
            description = "Returns paged notes. Admins can optionally include deleted notes."
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
        return noteService.findAll(pageable, q, tags, color, pinned);
    }

    @Operation(
            summary = "List deleted notes",
            description = "Returns paged soft-deleted notes for admins."
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
        return noteService.findDeleted(pageable, q, tags, color, pinned);
    }

    @Operation(
            summary = "Empty trash (admin)",
            description = "Permanently deletes all soft-deleted notes."
    )
    @ApiResponse(responseCode = "204", description = "Trash emptied")
    @DeleteMapping("/deleted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emptyTrash() {
        noteService.emptyTrash();
    }

    @Operation(
            summary = "Update note (admin)",
            description = "Updates title and content of a note."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note updated",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found")
    @PutMapping("/{id}")
    public NoteDTO update(
            @Parameter(description = "Note identifier") @PathVariable Long id,
            @Valid @RequestBody NoteUpdateRequest request
    ) {
        return noteService.update(id, request);
    }

    @Operation(
            summary = "Patch note (admin)",
            description = "Partially updates a note."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note patched",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found")
    @PatchMapping("/{id}")
    public NoteDTO patch(
            @Parameter(description = "Note identifier") @PathVariable Long id,
            @Valid @RequestBody NotePatchRequest request
    ) {
        return noteService.patch(id, request);
    }

    @Operation(
            summary = "Delete note (admin)",
            description = "Deletes a note by id."
    )
    @ApiResponse(responseCode = "204", description = "Note deleted")
    @ApiResponse(responseCode = "404", description = "Note not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.delete(id);
    }

    @Operation(
            summary = "Bulk actions on notes (admin)",
            description = "Performs bulk soft delete, restore, or permanent delete on provided ids."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Bulk action result",
            content = @Content(schema = @Schema(implementation = BulkActionResult.class))
    )
    @PostMapping("/bulk")
    public BulkActionResult bulk(@Valid @RequestBody BulkActionRequest request) {
        return noteService.bulk(request);
    }

    @Operation(
            summary = "Restore note (admin)",
            description = "Restores a soft-deleted note."
    )
    @ApiResponse(responseCode = "204", description = "Note restored")
    @ApiResponse(responseCode = "404", description = "Note not found")
    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.restore(id);
    }

    @Operation(
            summary = "Delete note permanently (admin)",
            description = "Permanently deletes a soft-deleted note."
    )
    @ApiResponse(responseCode = "204", description = "Note permanently deleted")
    @ApiResponse(responseCode = "404", description = "Note not found")
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@Parameter(description = "Note identifier") @PathVariable Long id) {
        noteService.deletePermanently(id);
    }

    @Operation(
            summary = "Get note (admin)",
            description = "Fetches a single note by id."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note found",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found")
    @GetMapping("/{id}")
    public NoteDTO findById(@Parameter(description = "Note identifier") @PathVariable Long id) {
        return noteService.findById(id);
    }

    @Operation(
            summary = "List revisions of a note (admin)",
            description = "Returns historical revisions for a note including snapshots."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Revisions returned",
            content = @Content(schema = @Schema(implementation = NoteRevisionDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Note not found")
    @GetMapping("/{id}/revisions")
    public Page<NoteRevisionDTO> findRevisions(@Parameter(description = "Note identifier") @PathVariable Long id,
                                               @ParameterObject Pageable pageable) {
        return noteService.findRevisions(id, pageable);
    }

    @Operation(
            summary = "Get a single revision snapshot (admin)",
            description = "Fetches the note snapshot at a specific revision."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Revision found",
            content = @Content(schema = @Schema(implementation = NoteRevisionDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Revision not found")
    @GetMapping("/{id}/revisions/{revisionId}")
    public NoteRevisionDTO findRevision(@Parameter(description = "Note identifier") @PathVariable Long id,
                                        @Parameter(description = "Revision number") @PathVariable Long revisionId) {
        return noteService.findRevision(id, revisionId);
    }

    @Operation(
            summary = "Restore note to a revision (admin)",
            description = "Restores the current note to the content of the specified revision."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Note restored",
            content = @Content(schema = @Schema(implementation = NoteDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Revision not found")
    @PostMapping("/{id}/revisions/{revisionId}/restore")
    public NoteDTO restoreRevision(@Parameter(description = "Note identifier") @PathVariable Long id,
                                   @Parameter(description = "Revision number") @PathVariable Long revisionId) {
        return noteService.restoreRevision(id, revisionId);
    }
}
