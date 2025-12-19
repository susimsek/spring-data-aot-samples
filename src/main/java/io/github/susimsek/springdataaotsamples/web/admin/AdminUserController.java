package io.github.susimsek.springdataaotsamples.web.admin;

import io.github.susimsek.springdataaotsamples.service.dto.UserSearchDTO;
import io.github.susimsek.springdataaotsamples.service.query.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "admin-users", description = "Admin-only user search APIs")
public class AdminUserController {

    private final UserQueryService userQueryService;

    @Operation(
            summary = "Search users",
            description = "Returns usernames matching the query. Case-insensitive contains search.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Paged usernames",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = UserSearchDTO.class)))
            })
    @GetMapping("/search")
    public Page<UserSearchDTO> search(
            @RequestParam(value = "q", required = false)
                    @Parameter(description = "Username fragment")
                    String query,
            @ParameterObject Pageable pageable) {
        return userQueryService.searchUsernames(query, pageable);
    }
}
