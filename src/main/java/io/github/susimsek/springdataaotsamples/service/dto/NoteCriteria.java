package io.github.susimsek.springdataaotsamples.service.dto;

import java.util.Set;
import org.jspecify.annotations.Nullable;

public record NoteCriteria(
    @Nullable String query,
    @Nullable Boolean deleted,
    @Nullable Set<String> tags,
    @Nullable String color,
    @Nullable Boolean pinned,
    @Nullable String owner) {}
