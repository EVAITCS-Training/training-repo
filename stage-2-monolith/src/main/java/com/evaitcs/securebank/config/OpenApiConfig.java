package com.evaitcs.securebank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OPENAPI / SWAGGER UI (Week 7: "API docs that generate themselves").
 *
 * springdoc introspects the controllers at runtime and serves:
 *   /v3/api-docs     — the OpenAPI 3 JSON contract
 *   /swagger-ui.html — interactive UI (try requests in the browser)
 *
 * The bean below only adds METADATA + the Bearer scheme, so the UI grows an
 * "Authorize" button: paste the token from POST /api/auth/login once and
 * every "Try it out" request sends it. Generated docs cannot drift from the
 * code — they ARE the code, which no hand-written Postman doc can promise.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI secureBankOpenApi() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("SecureBank API (Stage 2 — Modular Monolith)")
                        .version("1.0.0")
                        .description("""
                                Online banking: accounts, transfers, statements, loans.
                                Login at /api/auth/login (demo users: alice/password123,
                                teller/password123), then Authorize with the returned token.
                                """))
                // Declare that endpoints expect "Authorization: Bearer <JWT>"...
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // ...and apply that requirement globally.
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
