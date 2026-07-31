package br.com.nucleodasreformas.nucleoerp.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {

        return new OpenAPI()

                .info(new Info()

                        .title("Núcleo ERP API")

                        .description("""
                                API responsável pelo gerenciamento do ERP
                                da Núcleo das Reformas.
                                """)

                        .version("1.0.0")

                        .contact(new Contact()

                                .name("Luciano")

                                .email("lucianopovoasc@gmail.com"))

                        .license(new License()

                                .name("MIT")))

                .externalDocs(new ExternalDocumentation()

                        .description("Documentação"));

    }

}