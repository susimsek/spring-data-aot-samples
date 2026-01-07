package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    @Test
    void defaultsShouldMatchApplicationDefaults() {
        ApplicationProperties props = new ApplicationProperties();

        assertThat(props.getSecurity().getContentSecurityPolicy())
                .isEqualTo(ApplicationDefaults.Security.contentSecurityPolicy);
        assertThat(props.getSecurity().getJwt().getAccessTokenTtl())
                .isEqualTo(ApplicationDefaults.Security.Jwt.accessTokenTtl);
        assertThat(props.getSecurity().getJwt().getRefreshTokenTtl())
                .isEqualTo(ApplicationDefaults.Security.Jwt.refreshTokenTtl);
        assertThat(props.getSecurity().getJwt().getRefreshTokenTtlForRememberMe())
                .isEqualTo(ApplicationDefaults.Security.Jwt.refreshTokenTtlForRememberMe);
        assertThat(props.getSecurity().getJwt().getAudience())
                .isEqualTo(ApplicationDefaults.Security.Jwt.audience);

        assertThat(props.getCache().getCaffeine().getTtl())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.ttl);
        assertThat(props.getCache().getCaffeine().getInitialCapacity())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.initialCapacity);
        assertThat(props.getCache().getCaffeine().getMaximumSize())
                .isEqualTo(ApplicationDefaults.Cache.Caffeine.maximumSize);

        assertThat(props.getApiDocs().getTitle()).isEqualTo(ApplicationDefaults.ApiDocs.title);
        assertThat(props.getApiDocs().getDescription())
                .isEqualTo(ApplicationDefaults.ApiDocs.description);
        assertThat(props.getApiDocs().getVersion()).isEqualTo(ApplicationDefaults.ApiDocs.version);
    }

    @Test
    void settersShouldOverrideValues() {
        ApplicationProperties props = new ApplicationProperties();

        props.getSecurity().getJwt().setAccessTokenTtl(Duration.ofMinutes(5));
        props.getSecurity().getJwt().setAudience(List.of("api"));
        props.getCache().getCaffeine().setMaximumSize(42);
        props.getApiDocs().setTitle("Custom");

        assertThat(props.getSecurity().getJwt().getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(props.getSecurity().getJwt().getAudience()).containsExactly("api");
        assertThat(props.getCache().getCaffeine().getMaximumSize()).isEqualTo(42);
        assertThat(props.getApiDocs().getTitle()).isEqualTo("Custom");
    }
}
