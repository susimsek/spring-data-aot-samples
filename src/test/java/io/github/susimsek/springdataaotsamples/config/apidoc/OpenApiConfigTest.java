package io.github.susimsek.springdataaotsamples.config.apidoc;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.web.method.HandlerMethod;

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

    @Test
    void notesOpenApiShouldUseApplicationPropertiesAndConfigureSecurityAndServers() {
        ApplicationProperties props = new ApplicationProperties();
        var apiDocs = props.getApiDocs();
        apiDocs.setTitle("Notes API");
        apiDocs.setDescription("API description");
        apiDocs.setVersion("1.2.3");
        apiDocs.setTermsOfServiceUrl("https://example.com/tos");
        apiDocs.setContactName("Contact");
        apiDocs.setContactUrl("https://example.com");
        apiDocs.setContactEmail("contact@example.com");
        apiDocs.setLicense("Apache-2.0");
        apiDocs.setLicenseUrl("https://example.com/license");

        ApplicationProperties.ApiDocs.Server server = new ApplicationProperties.ApiDocs.Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Local");
        apiDocs.setServers(List.of(server));

        OpenApiConfig config = new OpenApiConfig(props);
        OpenAPI openApi = config.notesOpenApi();

        assertThat(openApi.getInfo()).isNotNull();
        assertThat(openApi.getInfo().getTitle()).isEqualTo("Notes API");
        assertThat(openApi.getInfo().getDescription()).isEqualTo("API description");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.2.3");
        assertThat(openApi.getInfo().getTermsOfService()).isEqualTo("https://example.com/tos");
        assertThat(openApi.getInfo().getContact()).isNotNull();
        assertThat(openApi.getInfo().getContact().getName()).isEqualTo("Contact");
        assertThat(openApi.getInfo().getLicense()).isNotNull();
        assertThat(openApi.getInfo().getLicense().getName()).isEqualTo("Apache-2.0");

        assertThat(openApi.getComponents()).isNotNull();
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearer-jwt");
        assertThat(openApi.getSecurity()).isNotEmpty();

        assertThat(openApi.getServers()).hasSize(1);
        assertThat(openApi.getServers().getFirst().getUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void securityResponsesCustomizerShouldAdd400401403500WhenValidAndSecured() throws Exception {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());
        Operation operation = new Operation().responses(new ApiResponses());

        HandlerMethod handlerMethod = handlerMethod(DummyController.class, "securedWithValid");

        config.securityResponsesCustomizer().customize(operation, handlerMethod);

        assertThat(operation.getResponses()).containsKeys("400", "401", "403", "500");
    }

    @Test
    void securityResponsesCustomizerShouldNotAdd401403WhenSecurityRequirementsClearsSecurity()
            throws Exception {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());
        Operation operation = new Operation().responses(new ApiResponses());

        HandlerMethod handlerMethod = handlerMethod(DummyController.class, "publicWithValid");

        config.securityResponsesCustomizer().customize(operation, handlerMethod);

        assertThat(operation.getResponses()).containsKeys("400", "500");
        assertThat(operation.getResponses()).doesNotContainKeys("401", "403");
    }

    @Test
    void securityResponsesCustomizerShouldNotAdd400WhenNoValidParameter() throws Exception {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());
        Operation operation = new Operation().responses(new ApiResponses());

        HandlerMethod handlerMethod = handlerMethod(DummyController.class, "securedNoValid");

        config.securityResponsesCustomizer().customize(operation, handlerMethod);

        assertThat(operation.getResponses()).containsKeys("401", "403", "500");
        assertThat(operation.getResponses()).doesNotContainKeys("400");
    }

    @Test
    void securityResponsesCustomizerShouldNotOverwriteExisting400() throws Exception {
        OpenApiConfig config = new OpenApiConfig(new ApplicationProperties());
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse());
        Operation operation = new Operation().responses(responses);

        HandlerMethod handlerMethod = handlerMethod(DummyController.class, "securedWithValid");

        config.securityResponsesCustomizer().customize(operation, handlerMethod);

        assertThat(operation.getResponses()).containsKeys("400", "401", "403", "500");
    }

    private static HandlerMethod handlerMethod(Class<?> type, String methodName) throws Exception {
        Method method = type.getDeclaredMethod(methodName, Object.class);
        return new HandlerMethod(type.getDeclaredConstructor().newInstance(), method);
    }

    private static final class DummyController {
        void securedWithValid(@Valid Object body) {
            Objects.requireNonNull(body, "body");
            throw new UnsupportedOperationException("Test stub");
        }

        @SecurityRequirements
        void publicWithValid(@Valid Object body) {
            Objects.requireNonNull(body, "body");
            throw new UnsupportedOperationException("Test stub");
        }

        void securedNoValid(Object body) {
            Objects.requireNonNull(body, "body");
            throw new UnsupportedOperationException("Test stub");
        }
    }
}
