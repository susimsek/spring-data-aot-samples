package io.github.susimsek.springdataaotsamples.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.github.susimsek.springdataaotsamples.config.Constants.AUDITOR_HEADER;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI notesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Note API")
                        .version("v1")
                        .description("Note endpoints with CRUD operations.")
                        .contact(new Contact().name("codex")))
                .servers(List.of(new Server().url("/").description("Default server")));
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> operation.addParametersItem(new Parameter()
                        .name(AUDITOR_HEADER)
                        .in("header")
                        .description("Optional auditor header")
                        .required(false)
                        .schema(new StringSchema()
                                ._default(Constants.DEFAULT_AUDITOR)
                                .example("alice"))))
        );
    }
}
