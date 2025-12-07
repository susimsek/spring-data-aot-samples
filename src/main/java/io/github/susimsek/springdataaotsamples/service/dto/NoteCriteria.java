package io.github.susimsek.springdataaotsamples.service.dto;

import org.jspecify.annotations.Nullable;

import java.util.Set;

public record NoteCriteria(
    @Nullable String query,
    @Nullable Boolean deleted,
    @Nullable Set<String> tags,
    @Nullable String color,
    @Nullable Boolean pinned,
    @Nullable String createdBy) {
}
