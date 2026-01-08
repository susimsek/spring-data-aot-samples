package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationDefaults;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class CacheConfigTest {

    private final ApplicationProperties props = buildProps();
    private final CacheConfig cacheConfig = new CacheConfig(props);

    @Test
    void cacheManagerShouldUseCaffeine() {
        CacheManager cacheManager = cacheConfig.cacheManager();
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
    }

    @Test
    void caffeineBuilderShouldUseApplicationProperties() {
        ApplicationProperties.Caffeine caffeineProps = props.getCache().getCaffeine();
        CacheManager cm = cacheConfig.cacheManager();
        assertThat(cm).isInstanceOf(CaffeineCacheManager.class);
        assertThat(caffeineProps.getTtl()).isEqualTo(ApplicationDefaults.Cache.Caffeine.ttl);
        assertThat(caffeineProps.getInitialCapacity())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.initialCapacity);
        assertThat(caffeineProps.getMaximumSize())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.maximumSize);
    }

    private ApplicationProperties buildProps() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getCache().getCaffeine().setTtl(ApplicationDefaults.Cache.Caffeine.ttl);
        properties
                .getCache()
                .getCaffeine()
                .setInitialCapacity(ApplicationDefaults.Cache.Caffeine.initialCapacity);
        properties
                .getCache()
                .getCaffeine()
                .setMaximumSize(ApplicationDefaults.Cache.Caffeine.maximumSize);
        return properties;
    }
}
