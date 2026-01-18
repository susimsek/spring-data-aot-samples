package io.github.susimsek.springdataaotsamples.config.apidoc;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

class OpenApiConfigTest {

    @Test
    void shouldDefineGroupedApis() {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());

        GroupedOpenApi authentication = config.authenticationApi();
        assertThat(authentication.getGroup()).isEqualTo("authentication");
        assertThat(authentication.getPathsToMatch()).contains("/api/auth/**");

        GroupedOpenApi core = config.coreApi();
        assertThat(core.getGroup()).isEqualTo("core");
        assertThat(core.getPathsToMatch())
                .contains("/api/notes/**", "/api/tags/**", "/api/share/**");

        GroupedOpenApi admin = config.adminApi();
        assertThat(admin.getGroup()).isEqualTo("admin");
        assertThat(admin.getPathsToMatch()).contains("/api/admin/**");
    }
}
