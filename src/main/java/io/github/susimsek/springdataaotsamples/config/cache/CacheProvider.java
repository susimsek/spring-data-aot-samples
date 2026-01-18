package io.github.susimsek.springdataaotsamples.config.cache;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheProvider {

    private final CacheManager cacheManager;

    public void clearCache(String cacheName, Object key) {
        Cache cache =
                Objects.requireNonNull(
                        cacheManager.getCache(cacheName), cacheName + " cache not configured");
        cache.evictIfPresent(key);
    }

    public void clearCache(String cacheName, Iterable<?> keys) {
        Cache cache =
                Objects.requireNonNull(
                        cacheManager.getCache(cacheName), cacheName + " cache not configured");
        for (Object key : keys) {
            cache.evictIfPresent(key);
        }
    }
}
