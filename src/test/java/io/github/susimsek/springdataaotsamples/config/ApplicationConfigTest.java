package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

class ApplicationConfigTest {

    @Test
    void shouldEnableApplicationProperties() {
        EnableConfigurationProperties annotation =
                ApplicationConfig.class.getAnnotation(EnableConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(ApplicationProperties.class);
    }

    @Test
    void shouldDisableProxyBeanMethods() {
        Configuration annotation = ApplicationConfig.class.getAnnotation(Configuration.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.proxyBeanMethods()).isFalse();
    }
}
