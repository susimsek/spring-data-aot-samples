package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.InitializingBean;

class OpenTelemetryConfigTest {

    @Test
    void initializerShouldInstallAppender() throws Exception {
        OpenTelemetryConfig config = new OpenTelemetryConfig();
        OpenTelemetry telemetry = mock(OpenTelemetry.class);

        try (MockedStatic<OpenTelemetryAppender> appender =
                Mockito.mockStatic(OpenTelemetryAppender.class)) {
            InitializingBean bean = config.openTelemetryAppenderInitializer(telemetry);
            bean.afterPropertiesSet();

            appender.verify(() -> OpenTelemetryAppender.install(telemetry));
            assertThat(bean).isNotNull();
        }
    }
}
