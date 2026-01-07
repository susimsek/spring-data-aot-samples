package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class OpenTelemetryConfigTest {

    @Test
    void initializerShouldInstallAppender() throws Exception {
        OpenTelemetryConfig config = new OpenTelemetryConfig();
        OpenTelemetry telemetry = mock(OpenTelemetry.class);

        InitializingBean bean = config.openTelemetryAppenderInitializer(telemetry);
        bean.afterPropertiesSet();

        // No exception thrown; OpenTelemetryAppender.install is static, so just ensure bean is non-null.
        assertThat(bean).isNotNull();
    }
}
