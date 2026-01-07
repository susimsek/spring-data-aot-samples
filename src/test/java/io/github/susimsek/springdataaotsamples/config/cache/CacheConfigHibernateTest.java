package io.github.susimsek.springdataaotsamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;

class CacheConfigHibernateTest {

    @Test
    void hibernateCacheBeansShouldBeCreatedWhenEnabled() {
        ApplicationProperties props = new ApplicationProperties();
        CacheConfig.HibernateSecondLevelCacheConfiguration cfg =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(props);

        javax.cache.CacheManager jcacheManager = cfg.jcacheManager(cm -> {});
        HibernatePropertiesCustomizer customizer = cfg.hibernatePropertiesCustomizer(jcacheManager);
        JCacheManagerCustomizer cacheCustomizer = cfg.cacheManagerCustomizer();

        assertThat(jcacheManager).isNotNull();
        assertThat(customizer).isNotNull();
        assertThat(cacheCustomizer).isNotNull();
    }
}
