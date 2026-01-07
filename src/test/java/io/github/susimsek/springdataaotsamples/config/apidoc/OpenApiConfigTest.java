package io.github.susimsek.springdataaotsamples.config.apidoc;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class OpenApiConfigTest {

    @Test
    void notesOpenApiShouldIncludeSecurityScheme() {
        ApplicationProperties props = new ApplicationProperties();
        props.getApiDocs().setTitle("Notes");
        OpenApiConfig config = new OpenApiConfig(props);

        OpenAPI api = config.notesOpenApi();

        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearer-jwt");
        assertThat(api.getInfo().getTitle()).isEqualTo("Notes");
    }

    @Test
    void securityResponsesCustomizerShouldAddResponsesWhenSecured() throws NoSuchMethodException {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());
        var customizer = config.securityResponsesCustomizer();
        HandlerMethod method =
                new HandlerMethod(this, this.getClass().getDeclaredMethod("securedMethod"));
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        customizer.customize(operation, method);
        assertThat(operation.getResponses().get("401")).isNotNull();
        assertThat(operation.getResponses().get("403")).isNotNull();
        assertThat(operation.getResponses().get("500")).isNotNull();
    }

    // no annotation so security responses are added by default
    void securedMethod() {}
}
