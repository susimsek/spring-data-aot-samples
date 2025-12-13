package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/share", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "share", description = "Public share token APIs")
@RequiredArgsConstructor
public class ShareApiController {

    private final NoteShareService noteShareService;
    private final NoteMapper noteMapper;

    @Operation(
            summary = "Get shared note",
            description = "Returns a note by share token."
    )
    @ApiResponse(responseCode = "200", description = "Note returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NoteDTO.class)))
    @GetMapping("/{token}")
    public NoteDTO getShared(@PathVariable String token) {
        var shareToken = noteShareService.validateAndConsume(token);
        var note = shareToken.getNote();
        return noteMapper.toDto(note);
    }
}
