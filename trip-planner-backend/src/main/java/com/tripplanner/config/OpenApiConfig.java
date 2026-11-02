package com.tripplanner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI tripPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Trip Planner API")
                        .version("v1")
                        .description(
                                "Full-stack AI trip planner backend: auth, trip CRUD, AI itinerary "
                                        + "generation (Gemini), real map/places data (Google Maps), route "
                                        + "optimization, budget planning, favorites, sharing, destination "
                                        + "discovery, and nearby hotel search. "
                                        + "Click 'Authorize' below and paste a JWT (from POST /api/auth/login "
                                        + "or /register) as 'Bearer <token>' to test protected endpoints."))
                // Registers the padlock icon on every endpoint and lets "Authorize"
                // apply the token to all subsequent requests made from this UI.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
