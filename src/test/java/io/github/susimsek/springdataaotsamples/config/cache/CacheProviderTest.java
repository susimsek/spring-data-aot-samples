package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CacheProviderTest {

    @Test
    void clearCacheShouldEvictIfPresent() {
        Cache cache = mock(Cache.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("test")).thenReturn(cache);

        CacheProvider provider = new CacheProvider(cacheManager);
        provider.clearCache("test", "k");

        verify(cache).evictIfPresent("k");
    }

    @Test
    void clearCacheIterableShouldEvictIfPresent() {
        Cache cache = mock(Cache.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("test")).thenReturn(cache);

        CacheProvider provider = new CacheProvider(cacheManager);
        provider.clearCache("test", List.of("a", "b"));

        verify(cache).evictIfPresent("a");
        verify(cache).evictIfPresent("b");
    }

    @Test
    void clearCacheShouldThrowWhenCacheMissing() {
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("missing")).thenReturn(null);

        CacheProvider provider = new CacheProvider(cacheManager);

        assertThatThrownBy(() -> provider.clearCache("missing", "k"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("missing cache not configured");
    }
}
