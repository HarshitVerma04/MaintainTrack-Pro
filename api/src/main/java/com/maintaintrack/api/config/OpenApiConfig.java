package com.maintaintrack.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MaintainTrack Pro API")
                        .description(
                                "REST API for MaintainTrack Pro — a Cloud + Desktop hybrid " +
                                        "facility maintenance management system. " +
                                        "All endpoints except /auth/login and /auth/register require a Bearer JWT token.")
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("Harshit")
                                .email("harshit@maintaintrack.com"))
                        .license(new License()
                                .name("Internal: CCL Internship Project")))
                // Adds the Authorize button to Swagger UI
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .name("Bearer Token")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}