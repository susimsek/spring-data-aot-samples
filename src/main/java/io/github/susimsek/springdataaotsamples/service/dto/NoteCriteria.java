package io.github.susimsek.springdataaotsamples.service.dto;

import org.jspecify.annotations.Nullable;

public record NoteCriteria(
    @Nullable String query,
    @Nullable Boolean deleted) {
}
