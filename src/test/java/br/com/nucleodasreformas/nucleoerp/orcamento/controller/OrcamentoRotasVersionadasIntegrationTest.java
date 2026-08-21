package br.com.nucleodasreformas.nucleoerp.orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrcamentoRotasVersionadasIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;

    @Test
    void deveExporVersaoAtualERotasExplicitamenteVersionadas() throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente rota " + UUID.randomUUID()).build());

        String response = mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"observacao":"Proposta inicial"}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").isNumber())
                .andExpect(jsonPath("$.versaoAtual.numeroVersao").value(1))
                .andExpect(jsonPath("$.versaoAtual.status.codigo").value("RASCUNHO"))
                .andExpect(jsonPath("$.totalComercial").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response);
        long orcamentoId = json.get("id").asLong();
        long versaoId = json.get("versaoAtual").get("id").asLong();

        mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}",
                        orcamentoId, versaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("Proposta inicial"))
                .andExpect(jsonPath("$.totalComercial").value(0.00))
                .andExpect(jsonPath("$.margemPrevista").value(0.00));

        for (String recurso : new String[]{"itens", "materiais", "mao-de-obra", "despesas"}) {
            mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}/" + recurso,
                            orcamentoId, versaoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            mockMvc.perform(get("/orcamentos/{orcamentoId}/" + recurso, orcamentoId))
                    .andExpect(status().isNotFound());
        }
    }
}
