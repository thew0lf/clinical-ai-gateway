package com.clinicalai.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String SCHEME = "bearer-jwt";

  @Bean
  public OpenAPI clinicalAiGatewayOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Clinical AI Control Plane")
                .version("0.1.0")
                .description(
                    "Governance gateway for clinical AI assistants: PHI redaction, per tenant"
                        + " usage metering, configurable human review, and a Kafka audit"
                        + " trail."))
        .addSecurityItem(new SecurityRequirement().addList(SCHEME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
