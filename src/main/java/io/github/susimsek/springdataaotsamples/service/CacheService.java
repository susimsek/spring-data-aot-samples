package io.github.susimsek.springdataaotsamples.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    public void clearCaches(String... cacheNames) {
        Arrays.stream(cacheNames).forEach(this::clearCache);
    }

    public void clearCache(String cacheName) {
        Objects.requireNonNull(
            cacheManager.getCache(cacheName),
            cacheName + " cache not configured"
        ).clear();
    }
}
