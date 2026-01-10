package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class ApplicationPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ConfigurationPropertiesAutoConfiguration.class,
                                    ValidationAutoConfiguration.class))
                    .withUserConfiguration(TestConfig.class);

    @Test
    void shouldFailStartupWhenJwtSecretMissing() {
        contextRunner
                .withPropertyValues(
                        "application.security.jwt.issuer=http://issuer",
                        "application.security.jwt.audience[0]=aud")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable rootCause = rootCause(context.getStartupFailure());
                            assertThat(rootCause.getMessage())
                                    .contains("security.jwt.secret")
                                    .contains("must not be blank");
                        });
    }

    @Test
    void shouldFailStartupWhenJwtIssuerMissing() {
        contextRunner
                .withPropertyValues(
                        "application.security.jwt.secret=secret",
                        "application.security.jwt.audience[0]=aud")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable rootCause = rootCause(context.getStartupFailure());
                            assertThat(rootCause.getMessage())
                                    .contains("security.jwt.issuer")
                                    .contains("must not be blank");
                        });
    }

    @Test
    void shouldStartWhenJwtSecretAndIssuerProvided() {
        contextRunner
                .withPropertyValues(
                        "application.security.jwt.issuer=http://issuer",
                        "application.security.jwt.secret=secret",
                        "application.security.jwt.audience[0]=aud")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldFailStartupWhenJwtAudienceMissing() {
        contextRunner
                .withPropertyValues(
                        "application.security.jwt.issuer=http://issuer",
                        "application.security.jwt.secret=secret")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable rootCause = rootCause(context.getStartupFailure());
                            assertThat(rootCause.getMessage())
                                    .contains("security.jwt.audience")
                                    .contains("must not be empty");
                        });
    }

    @Test
    void shouldFailStartupWhenJwtAudienceContainsBlankValue() {
        contextRunner
                .withPropertyValues(
                        "application.security.jwt.issuer=http://issuer",
                        "application.security.jwt.secret=secret",
                        "application.security.jwt.audience[0]= ")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable rootCause = rootCause(context.getStartupFailure());
                            assertThat(rootCause.getMessage())
                                    .contains("security.jwt.audience[0]")
                                    .contains("must not be blank");
                        });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig extends ApplicationConfig {}
}
