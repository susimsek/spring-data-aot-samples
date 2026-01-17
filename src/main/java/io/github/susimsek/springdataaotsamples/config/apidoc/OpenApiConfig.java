package io.github.susimsek.springdataaotsamples.config.apidoc;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.web.error.ProblemDetail;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "springdoc.api-docs",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OpenApiConfig {

    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/ProblemDetail";

    private final ApplicationProperties applicationProperties;

    @Bean
    public OpenAPI notesOpenApi() {
        ApplicationProperties.ApiDocs apiDocs = applicationProperties.getApiDocs();
        return new OpenAPI()
                .info(buildInfo(apiDocs))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearer-jwt",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .servers(buildServers(apiDocs.getServers()));
    }

    @Bean
    public OperationCustomizer securityResponsesCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            addValidationErrorResponse(operation, handlerMethod);
            addSecurityErrorResponse(operation, handlerMethod);
            addDefaultErrorResponse(operation);
            return operation;
        };
    }

    private void addValidationErrorResponse(Operation operation, HandlerMethod handlerMethod) {
        boolean hasValidParameter =
                Arrays.stream(handlerMethod.getMethodParameters())
                        .anyMatch(parameter -> parameter.hasParameterAnnotation(Valid.class));
        if (!hasValidParameter) {
            return;
        }
        if (operation.getResponses() != null && operation.getResponses().containsKey("400")) {
            return;
        }
        operation
                .getResponses()
                .addApiResponse(
                        "400",
                        new ApiResponse()
                                .description("Invalid input")
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        org.springframework.http.MediaType
                                                                .APPLICATION_JSON_VALUE,
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<ProblemDetail>()
                                                                                .$ref(
                                                                                        PROBLEM_DETAIL_REF))
                                                                .example(
                                                                        getInvalidInputExample()))));
    }

    private void addSecurityErrorResponse(Operation operation, HandlerMethod handlerMethod) {
        SecurityRequirements sr =
                handlerMethod.getMethod().getAnnotation(SecurityRequirements.class);
        boolean hasSecurity = (sr == null || sr.value().length > 0);
        if (!hasSecurity) {
            return;
        }
        operation
                .getResponses()
                .addApiResponse(
                        "401",
                        new ApiResponse()
                                .description("Unauthorized")
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        org.springframework.http.MediaType
                                                                .APPLICATION_JSON_VALUE,
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<ProblemDetail>()
                                                                                .$ref(
                                                                                        PROBLEM_DETAIL_REF))
                                                                .example(
                                                                        getUnauthorizedExample()))));
        operation
                .getResponses()
                .addApiResponse(
                        "403",
                        new ApiResponse()
                                .description("Access Denied")
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        org.springframework.http.MediaType
                                                                .APPLICATION_JSON_VALUE,
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<ProblemDetail>()
                                                                                .$ref(
                                                                                        PROBLEM_DETAIL_REF))
                                                                .example(
                                                                        getAccessDeniedExample()))));
    }

    private void addDefaultErrorResponse(Operation operation) {
        operation
                .getResponses()
                .addApiResponse(
                        "500",
                        new ApiResponse()
                                .description("Internal Server Error")
                                .content(
                                        new Content()
                                                .addMediaType(
                                                        org.springframework.http.MediaType
                                                                .APPLICATION_JSON_VALUE,
                                                        new MediaType()
                                                                .schema(
                                                                        new Schema<ProblemDetail>()
                                                                                .$ref(
                                                                                        PROBLEM_DETAIL_REF))
                                                                .example(
                                                                        getDefaultErrorExample()))));
    }

    private Map<String, Object> getUnauthorizedExample() {
        return createProblemExample(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    private Map<String, Object> getAccessDeniedExample() {
        return createProblemExample(HttpStatus.FORBIDDEN, "Access is denied.");
    }

    private Map<String, Object> getDefaultErrorExample() {
        return createProblemExample(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private Map<String, Object> getInvalidInputExample() {
        Map<String, Object> example =
                createProblemExample(
                        HttpStatus.BAD_REQUEST, "One or more validation errors occurred.");
        example.put(
                "violations",
                List.of(
                        createViolationExample(
                                "NotBlank",
                                "registerRequest",
                                "username",
                                "",
                                "This field cannot be blank.")));
        return example;
    }

    private Map<String, Object> createViolationExample(
            String code, String objectName, String field, Object rejectedValue, String message) {
        Map<String, Object> violation = new LinkedHashMap<>();
        violation.put("code", code);
        violation.put("object", objectName);
        violation.put("field", field);
        violation.put("rejectedValue", rejectedValue);
        violation.put("message", message);
        return violation;
    }

    private Map<String, Object> createProblemExample(HttpStatus status, String detail) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", status.getReasonPhrase());
        example.put("status", status.value());
        example.put("detail", detail);
        example.put("instance", "/api/notes");
        return example;
    }

    private Info buildInfo(ApplicationProperties.ApiDocs apiDocs) {
        Contact contact =
                new Contact()
                        .name(apiDocs.getContactName())
                        .url(apiDocs.getContactUrl())
                        .email(apiDocs.getContactEmail());

        License license = new License().name(apiDocs.getLicense()).url(apiDocs.getLicenseUrl());

        Info info =
                new Info()
                        .title(apiDocs.getTitle())
                        .description(apiDocs.getDescription())
                        .version(apiDocs.getVersion())
                        .contact(contact)
                        .license(license);

        info.setTermsOfService(apiDocs.getTermsOfServiceUrl());
        return info;
    }

    private List<Server> buildServers(List<ApplicationProperties.ApiDocs.Server> servers) {
        return servers.stream()
                .map(
                        server ->
                                new Server()
                                        .url(server.getUrl())
                                        .description(server.getDescription()))
                .toList();
    }
}
