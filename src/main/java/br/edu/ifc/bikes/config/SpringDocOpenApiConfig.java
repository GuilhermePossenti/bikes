package br.edu.ifc.bikes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocOpenApiConfig {

    //nome do esquema de segurança usado nos @SecurityRequirement dos controllers
    public static final String SECURITY = "security";

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY, securityScheme()))
                .info(new Info()
                        .title("Bikes API")
                        .description("API para gestão de locadora de bikes.")
                        .version("V1")
                        .contact(new Contact()
                                .name("Fábio Pinheiro")
                                .email("fabio.pinheiro@ifc.edu.br")
                        )
                );
    }

    //adiciona o botão "Authorize" no Swagger para informar o token JWT
    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .description("Insira um bearer token válido para prosseguir")
                .type(SecurityScheme.Type.HTTP)
                .in(SecurityScheme.In.HEADER)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(SECURITY);
    }
}
