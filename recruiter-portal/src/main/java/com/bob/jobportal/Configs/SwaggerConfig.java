package com.bob.jobportal.Configs;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Recruiter Portal API")
                .version("v1")
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }

    @Bean
    public GroupedOpenApi recruiterApi() {
        return GroupedOpenApi.builder()
                .group("recruiter")
                .pathsToMatch("/api/v1/recruiter/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addParametersItem(new Parameter()
                            .in("header")
                            .required(true)
                            .name("X-Client")
                            .description("Client identifier (required: 'AzureAD')")
                            .schema(new StringSchema()._default("AzureAD"))
                    );
                    return operation;
                })
                .build();
    }
}