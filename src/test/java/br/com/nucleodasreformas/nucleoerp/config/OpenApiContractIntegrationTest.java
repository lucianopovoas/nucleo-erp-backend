package br.com.nucleodasreformas.nucleoerp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "springdoc.api-docs.path=/api-docs")
@AutoConfigureMockMvc
class OpenApiContractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void devePublicarRequestsResponsesErrosEAcoesPermitidas() throws Exception {
        String body = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode api = objectMapper.readTree(body);

        assertThat(api.at("/components/schemas/OrcamentoVersaoAcoesPermitidasResponse").isMissingNode())
                .isFalse();
        assertThat(api.at("/components/schemas/OrdemServicoAcoesPermitidasResponse").isMissingNode())
                .isFalse();
        assertThat(api.at("/components/schemas/ApiProblemDetail/properties/erros").isMissingNode())
                .isFalse();

        JsonNode versaoGet = api.at(
                "/paths/~1orcamentos~1{orcamentoId}~1versoes~1{versaoId}/get");
        assertThat(versaoGet.at("/parameters").size()).isEqualTo(2);
        assertThat(versaoGet.at("/responses/200").isMissingNode()).isFalse();
        assertThat(versaoGet.at("/responses/400").isMissingNode()).isFalse();
        assertThat(versaoGet.at("/responses/404").isMissingNode()).isFalse();

        JsonNode statusPut = api.at(
                "/paths/~1orcamentos~1{orcamentoId}~1versoes~1{versaoId}~1status/put");
        assertThat(statusPut.at("/requestBody/content/application~1json/schema").isMissingNode())
                .isFalse();

        JsonNode importacao = api.at("/paths/~1importacoes~1clientes/post/responses");
        assertThat(importacao.has("400")).isTrue();
        assertThat(importacao.has("413")).isTrue();
        assertThat(importacao.has("415")).isTrue();
    }
}
