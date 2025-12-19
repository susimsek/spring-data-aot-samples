package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import io.github.susimsek.springdataaotsamples.service.query.TagQueryService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/tags", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "tags", description = "Tag lookup APIs")
public class TagController {

  private final TagQueryService tagQueryService;

  @Operation(
      summary = "Suggest tags",
      description = "Returns paged tag names that start with the provided prefix.")
  @ApiResponse(
      responseCode = "200",
      description = "Suggested tag names",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = TagDTO.class)))
  @GetMapping("/suggest")
  public Page<TagDTO> suggest(
      @ParameterObject Pageable pageable,
      @RequestParam(value = "q", required = false)
          @Parameter(description = "Prefix to search tag names")
          String query) {
    if (!StringUtils.hasText(query) || query.trim().length() < 2) {
      return Page.empty(pageable);
    }
    var trimmed = query.trim();
    return tagQueryService.suggestPrefixPage(trimmed, pageable);
  }
}
