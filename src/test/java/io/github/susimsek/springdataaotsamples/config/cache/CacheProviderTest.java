package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CacheProviderTest {

    @Test
    void clearCachesShouldDelegateToCacheManager() {
        Cache cache = mock(Cache.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("test")).thenReturn(cache);

        CacheProvider provider = new CacheProvider(cacheManager);
        provider.clearCaches("test");

        verify(cache).clear();
    }

    @Test
    void clearCacheShouldThrowWhenCacheMissing() {
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("missing")).thenReturn(null);

        CacheProvider provider = new CacheProvider(cacheManager);

        assertThatThrownBy(() -> provider.clearCache("missing"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("missing cache not configured");
    }
}
