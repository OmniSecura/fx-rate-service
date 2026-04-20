package com.FXplore.fx_rate_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
        
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("FX Rate Service API")
                        .version("1.0")
                        .description("API for FX Rate Service"));

        // Add tag for health checks
        openAPI.addTagsItem(new Tag().name("health checks").description("Health check endpoints"));

        // Add actuator health endpoint manually
        Paths paths = new Paths();

        PathItem healthPath = new PathItem()
                .get(new Operation()
                        .tags(java.util.List.of("health checks"))
                        .summary("Health Check")
                        .description("Returns the health status of the application")
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("Application is healthy")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType()
                                                        .schema(new Schema<>().type("object")))))));
        paths.addPathItem("/actuator/health", healthPath);

        openAPI.paths(paths);

        return openAPI;
    }

    @Bean
    public GroupedOpenApi mainApi() {
        return GroupedOpenApi.builder()
                .group("main")
                .pathsToMatch("/api/**")
                .build();
    }
}