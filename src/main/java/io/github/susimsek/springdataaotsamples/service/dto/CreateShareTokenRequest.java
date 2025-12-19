package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.DateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Payload to create a share token for a single note")
public record CreateShareTokenRequest(
    @Schema(
            description = "Optional expiry time; default is 24 hours if not provided",
            example = "2024-12-31T23:59:59Z")
        @DateRange(minSeconds = 300, maxSeconds = 2_592_000)
        Instant expiresAt,
    @Schema(description = "If true, the token never expires", example = "false") Boolean noExpiry,
    @Schema(description = "Whether the link is one-time use", example = "false") Boolean oneTime) {}
