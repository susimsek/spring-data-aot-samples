package io.github.susimsek.springdataaotsamples.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a bulk action on notes")
public record BulkActionResult(
        @Schema(description = "Number of notes processed successfully", example = "3")
        int processedCount,

        @Schema(description = "IDs that failed to process", example = "[4,7]")
        List<Long> failedIds
) {
}
