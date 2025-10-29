package com.pneumaliback.www.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PneuMali - API de Vente de Pneu Au Mali")
                        .description(
                                "API REST pour la gestion d'un site de vente de pneu avec authentification JWT")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe PneuMali")
                                .email("contact@pneumali.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .description("Entrez votre token JWT")));
    }

}
