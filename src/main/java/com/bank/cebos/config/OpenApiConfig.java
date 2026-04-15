package com.bank.cebos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI cebosOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("CEBOS API")
                .description("Corporate Employee Bulk Onboarding System — REST API")
                .version("1.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "portalBearerJwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Portal JWT (`aud` must be `/api/v1/portal`)"))
                .addSecuritySchemes(
                    "mobileBearerJwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Mobile JWT (`aud` must be `/api/v1/mobile`)"))
                .addSecuritySchemes(
                    "adminBearerJwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Admin JWT (`aud` must be `/api/v1/admin`)")));
  }
}
