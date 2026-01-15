package io.github.susimsek.springdataaotsamples.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import java.util.OptionalLong;
import javax.cache.Caching;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final ApplicationProperties applicationProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(buildCaffeineConfig(cacheProperties()));
        return cacheManager;
    }

    private ApplicationProperties.Caffeine cacheProperties() {
        return applicationProperties.getCache().getCaffeine();
    }

    private Caffeine<Object, Object> buildCaffeineConfig(ApplicationProperties.Caffeine config) {
        return Caffeine.newBuilder()
                .expireAfterWrite(config.getTtl())
                .initialCapacity(config.getInitialCapacity())
                .maximumSize(config.getMaximumSize())
                .recordStats();
    }

    @ConditionalOnProperty(
            name = "spring.jpa.properties.hibernate.cache.use_second_level_cache",
            havingValue = "true")
    @RequiredArgsConstructor
    static class HibernateSecondLevelCacheConfiguration {

        private final ApplicationProperties applicationProperties;

        @Bean
        public javax.cache.CacheManager jcacheManager(JCacheManagerCustomizer customizer) {
            var provider = Caching.getCachingProvider(CaffeineCachingProvider.class.getName());
            var manager = provider.getCacheManager();
            customizer.customize(manager);
            return manager;
        }

        @Bean
        public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
                javax.cache.CacheManager jcacheManager) {
            return props -> props.put(ConfigSettings.CACHE_MANAGER, jcacheManager);
        }

        @Bean
        public JCacheManagerCustomizer cacheManagerCustomizer() {
            return cm -> {
                createCache(cm, "default-update-timestamps-region");
                createCache(cm, "default-query-results-region");
                createCache(cm, User.class.getName());
                createCache(cm, User.class.getName() + ".authorities");
                createCache(cm, Authority.class.getName());
                createCache(cm, Note.class.getName());
                createCache(cm, Note.class.getName() + ".tags");
                createCache(cm, NoteRepository.NOTE_BY_ID_CACHE);
                createCache(cm, NoteShareToken.class.getName());
                createCache(cm, NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE);
                createCache(cm, Tag.class.getName());
                createCache(cm, RefreshToken.class.getName());
                createCache(cm, UserRepository.USER_BY_USERNAME_CACHE);
            };
        }

        private void createCache(javax.cache.CacheManager cm, String cacheName) {
            var existing = cm.getCache(cacheName);
            if (existing != null) {
                existing.clear();
                return;
            }
            var config = cacheProperties();
            var caffeineConfig = new CaffeineConfiguration<>();
            caffeineConfig.setMaximumSize(OptionalLong.of(config.getMaximumSize()));
            caffeineConfig.setExpireAfterWrite(OptionalLong.of(config.getTtl().toNanos()));
            caffeineConfig.setStatisticsEnabled(true);
            cm.createCache(cacheName, caffeineConfig);
        }

        private ApplicationProperties.Caffeine cacheProperties() {
            return applicationProperties.getCache().getCaffeine();
        }
    }
}
