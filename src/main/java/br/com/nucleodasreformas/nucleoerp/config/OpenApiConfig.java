package br.com.nucleodasreformas.nucleoerp.config;

import br.com.nucleodasreformas.nucleoerp.config.openapi.ApiErrosComerciais;
import br.com.nucleodasreformas.nucleoerp.config.openapi.ApiErrosImportacao;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {

        Schema<?> problemSchema = new ObjectSchema()
                .description("Erro HTTP no formato RFC 9457; erros é opcional.")
                .addProperty("type", new StringSchema().format("uri").example("about:blank"))
                .addProperty("title", new StringSchema().example("Dados inválidos"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema()
                        .example("Um ou mais campos estão inválidos."))
                .addProperty("instance", new StringSchema().format("uri").example("/orcamentos"))
                .addProperty("erros", new ObjectSchema()
                        .description("Mensagens indexadas pelo nome do campo ou parâmetro."));

        return new OpenAPI()

                .components(new Components().addSchemas("ApiProblemDetail", problemSchema))

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

    @Bean
    public OperationCustomizer respostasProblemDetail() {
        return (operation, handlerMethod) -> {
            Class<?> controller = handlerMethod.getBeanType();
            if (AnnotatedElementUtils.hasAnnotation(controller, ApiErrosComerciais.class)) {
                operation.getResponses().addApiResponse("400", problemResponse(
                        "Entrada inválida, parâmetro incompatível ou regra de negócio não atendida"));
                boolean possuiPath = Arrays.stream(handlerMethod.getMethod().getParameters())
                        .anyMatch(parameter -> parameter.isAnnotationPresent(PathVariable.class));
                if (possuiPath) {
                    operation.getResponses().addApiResponse("404", problemResponse(
                            "Recurso inexistente no contexto informado"));
                }
            }
            if (AnnotatedElementUtils.hasAnnotation(controller, ApiErrosImportacao.class)) {
                operation.getResponses().addApiResponse("400", problemResponse(
                        "Multipart ausente/inválido, arquivo vazio ou arquivo não processável"));
                operation.getResponses().addApiResponse("413", problemResponse(
                        "Arquivo acima do limite configurado"));
                operation.getResponses().addApiResponse("415", problemResponse(
                        "Content-Type não suportado"));
            }
            return operation;
        };
    }

    private ApiResponse problemResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/problem+json",
                        new MediaType().schema(new Schema<>().$ref(
                                "#/components/schemas/ApiProblemDetail"))));
    }

}
