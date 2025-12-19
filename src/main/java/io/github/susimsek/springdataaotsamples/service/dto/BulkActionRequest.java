package io.github.susimsek.springdataaotsamples.service.dto;

import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.service.validation.constraints.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Bulk action payload for notes")
public record BulkActionRequest(
        @Schema(
                        description = "Bulk action type",
                        example = "DELETE_SOFT",
                        implementation = BulkAction.class)
                @NotNull
                @EnumValue(enumClass = BulkAction.class)
                String action,
        @Schema(description = "Note identifiers to apply the action to", example = "[1,2,3]")
                @NotEmpty
                @Size(max = 100)
                Set<Long> ids) {}
