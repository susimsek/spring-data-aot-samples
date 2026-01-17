package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.OptionalLong;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;

class CacheConfigHibernateTest {

    @Test
    void hibernateCacheBeansShouldBeCreatedWhenEnabled() {
        ApplicationProperties props = new ApplicationProperties();
        CacheConfig.HibernateSecondLevelCacheConfiguration cfg =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(props);

        CacheManager jcacheManager = cfg.jcacheManager(cm -> {});
        HibernatePropertiesCustomizer customizer = cfg.hibernatePropertiesCustomizer(jcacheManager);
        JCacheManagerCustomizer cacheCustomizer = cfg.cacheManagerCustomizer();

        assertThat(jcacheManager).isNotNull();
        assertThat(customizer).isNotNull();
        assertThat(cacheCustomizer).isNotNull();
    }

    @Test
    void createCacheShouldClearExistingBeforeReturning() throws Exception {
        ApplicationProperties props = new ApplicationProperties();
        CacheConfig.HibernateSecondLevelCacheConfiguration cfg =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(props);
        CacheManager cm = mock(CacheManager.class);
        Cache<?, ?> cache = mock(Cache.class);
        doReturn(cache).when(cm).getCache("existing");

        invokeCreateCache(cfg, cm, "existing");

        verify(cache).clear();
        verify(cm, never()).createCache(ArgumentMatchers.eq("existing"), ArgumentMatchers.any());
    }

    @Test
    void createCacheShouldUseCaffeineSettingsWhenMissing() throws Exception {
        ApplicationProperties props = new ApplicationProperties();
        props.getCache().getCaffeine().setMaximumSize(42);
        props.getCache().getCaffeine().setTtl(Duration.ofSeconds(5));
        CacheConfig.HibernateSecondLevelCacheConfiguration cfg =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(props);
        CacheManager cm = mock(CacheManager.class);
        when(cm.getCache("newCache")).thenReturn(null);
        ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);

        invokeCreateCache(cfg, cm, "newCache");

        verify(cm).createCache(ArgumentMatchers.eq("newCache"), captor.capture());
        CaffeineConfiguration<?, ?> caffeine = (CaffeineConfiguration<?, ?>) captor.getValue();
        assertThat(caffeine.getMaximumSize()).isEqualTo(OptionalLong.of(42));
        assertThat(caffeine.getExpireAfterWrite())
                .isEqualTo(OptionalLong.of(Duration.ofSeconds(5).toNanos()));
        assertThat(caffeine.isStatisticsEnabled()).isTrue();
    }

    @Test
    void cachePropertiesShouldReturnCaffeineSection() throws Exception {
        ApplicationProperties props = new ApplicationProperties();
        CacheConfig.HibernateSecondLevelCacheConfiguration cfg =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(props);

        Method method = cfg.getClass().getDeclaredMethod("cacheProperties");
        method.setAccessible(true);

        Object result = method.invoke(cfg);

        assertThat(result).isSameAs(props.getCache().getCaffeine());
    }

    private static void invokeCreateCache(
            CacheConfig.HibernateSecondLevelCacheConfiguration cfg, CacheManager cm, String name)
            throws Exception {
        Method method =
                cfg.getClass().getDeclaredMethod("createCache", CacheManager.class, String.class);
        method.setAccessible(true);
        method.invoke(cfg, cm, name);
    }
}
