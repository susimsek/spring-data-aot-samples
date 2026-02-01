package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import io.github.susimsek.springdataaotsamples.config.ApplicationDefaults;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import java.time.Duration;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.hibernate.cache.jcache.ConfigSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class CacheConfigTest {

    private final ApplicationProperties props = buildProps();
    private final CacheConfig cacheConfig = new CacheConfig(props);

    @Test
    void cacheManagerShouldUseCaffeine() {
        org.springframework.cache.CacheManager cacheManager = cacheConfig.cacheManager();
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
    }

    @Test
    void caffeineBuilderShouldUseApplicationProperties() {
        ApplicationProperties.Caffeine caffeineProps = props.getCache().getCaffeine();
        org.springframework.cache.CacheManager cm = cacheConfig.cacheManager();
        assertThat(cm).isInstanceOf(CaffeineCacheManager.class);
        assertThat(caffeineProps.getTtl()).isEqualTo(ApplicationDefaults.Cache.Caffeine.ttl);
        assertThat(caffeineProps.getInitialCapacity())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.initialCapacity);
        assertThat(caffeineProps.getMaximumSize())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.maximumSize);
    }

    @Test
    void cacheManagerShouldApplyCaffeineConfig() {
        org.springframework.cache.CacheManager cm = cacheConfig.cacheManager();
        assertThat(cm).isNotNull();
        CaffeineCacheManager caffeineCm = (CaffeineCacheManager) cm;
        assertThat(caffeineCm).isNotNull();
    }

    @Test
    void hibernateSecondLevelCacheConfigurationShouldCreateJCacheManager() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();
        assertThat(customizer).isNotNull();

        CacheManager jcacheManager = hibernateConfig.jcacheManager(customizer);
        assertThat(jcacheManager).isNotNull();
    }

    @Test
    void hibernatePropertiesCustomizerShouldSetCacheManager() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();
        CacheManager jcacheManager = hibernateConfig.jcacheManager(customizer);

        HibernatePropertiesCustomizer propsCustomizer =
                hibernateConfig.hibernatePropertiesCustomizer(jcacheManager);
        assertThat(propsCustomizer).isNotNull();

        var hibernateProperties = new java.util.HashMap<String, Object>();
        propsCustomizer.customize(hibernateProperties);
        assertThat(hibernateProperties).containsEntry(ConfigSettings.CACHE_MANAGER, jcacheManager);
    }

    @Test
    void cacheManagerCustomizerShouldRegisterAllCacheRegions() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();
        CacheManager cm = hibernateConfig.jcacheManager(customizer);

        assertThat(cm.getCache("default-update-timestamps-region")).isNotNull();
        assertThat(cm.getCache("default-query-results-region")).isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.User")).isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.User.authorities"))
                .isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.Authority"))
                .isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.Note")).isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.Note.tags"))
                .isNotNull();
        assertThat(cm.getCache(NoteRepository.NOTE_BY_ID_CACHE)).isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.NoteShareToken"))
                .isNotNull();
        assertThat(cm.getCache(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE))
                .isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.Tag")).isNotNull();
        assertThat(cm.getCache("io.github.susimsek.springdataaotsamples.domain.RefreshToken"))
                .isNotNull();
        assertThat(cm.getCache(UserRepository.USER_BY_USERNAME_CACHE)).isNotNull();
        assertThat(cm.getCache(UserRepository.USER_BY_EMAIL_CACHE)).isNotNull();
    }

    @Test
    void cacheManagerCustomizerShouldClearExistingCache() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();

        CacheManager cm = mock(CacheManager.class);
        Cache<Object, Object> existingCache = mock(Cache.class);
        when(cm.getCache(anyString())).thenReturn(existingCache);

        customizer.customize(cm);

        verify(existingCache, times(14)).clear();
    }

    @Test
    void cacheManagerCustomizerShouldCreateNewCacheWhenNotExisting() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();

        CacheManager cm = mock(CacheManager.class);
        when(cm.getCache(anyString())).thenReturn(null);

        customizer.customize(cm);

        verify(cm, times(14)).createCache(anyString(), any(CaffeineConfiguration.class));
    }

    @Test
    void caffeineConfigurationShouldUseCorrectValues() {
        ApplicationProperties hibernateProps = buildPropsWithSecondLevelCache();
        var hibernateConfig =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(hibernateProps);
        JCacheManagerCustomizer customizer = hibernateConfig.cacheManagerCustomizer();
        CacheManager cm = hibernateConfig.jcacheManager(customizer);

        Cache<Object, Object> cache = cm.getCache("default-update-timestamps-region");
        assertThat(cache).isNotNull();
    }

    @Test
    void cachePropertiesShouldReturnCaffeineConfig() {
        ApplicationProperties testProps = buildPropsWithSecondLevelCache();
        assertThat(testProps.getCache().getCaffeine()).isNotNull();
        assertThat(testProps.getCache().getCaffeine().getTtl())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.ttl);
        assertThat(testProps.getCache().getCaffeine().getMaximumSize())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.maximumSize);
    }

    @Test
    void buildCaffeineConfigShouldApplyAllProperties() {
        ApplicationProperties customProps = new ApplicationProperties();
        customProps.getCache().getCaffeine().setTtl(Duration.ofMinutes(10));
        customProps.getCache().getCaffeine().setInitialCapacity(200);
        customProps.getCache().getCaffeine().setMaximumSize(2000);

        CacheConfig testCacheConfig = new CacheConfig(customProps);
        org.springframework.cache.CacheManager cm = testCacheConfig.cacheManager();

        assertThat(cm).isNotNull().isInstanceOf(CaffeineCacheManager.class);
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

    private ApplicationProperties buildPropsWithSecondLevelCache() {
        return buildProps();
    }
}
