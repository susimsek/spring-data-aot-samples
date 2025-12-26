package io.github.susimsek.springdataaotsamples.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

@Configuration(proxyBeanMethods = false)
class OpenTelemetryConfig {

    private static final String OTEL_APPENDER_NAME = "OTEL";

    @Bean
    public OpenTelemetryAppender openTelemetryAppender(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
        LoggerContext context =
            (LoggerContext) LoggerFactory.getILoggerFactory();

        OpenTelemetryAppender appender = new OpenTelemetryAppender();
        appender.setContext(context);
        appender.setName(OTEL_APPENDER_NAME);
        appender.start();

        Logger root = context.getLogger(ROOT_LOGGER_NAME);
        root.addAppender(appender);

        return appender;
    }
}
