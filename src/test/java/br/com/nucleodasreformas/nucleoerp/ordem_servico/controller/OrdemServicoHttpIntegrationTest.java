package br.com.nucleodasreformas.nucleoerp.ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdemServicoHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private StatusOrcamentoRepository statusOrcamentoRepository;
    @Autowired private StatusOrdemServicoRepository statusOrdemServicoRepository;

    @Test
    void deveExecutarFluxoHttpCompletoDaAprovacaoAConclusao() throws Exception {
        Origem origem = criarOrcamento("Fluxo HTTP");
        alterarStatusComercial(origem, "ENVIADO");
        alterarStatusComercial(origem, "APROVADO");

        JsonNode ordem = criarOrdem(origem);
        long ordemId = ordem.get("id").asLong();
        long numero = ordem.get("numero").asLong();

        mockMvc.perform(get("/ordens-servico/{id}", ordemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ordemId))
                .andExpect(jsonPath("$.numero").value(numero))
                .andExpect(jsonPath("$.status.codigo").value("COMPRAR_MATERIAL"))
                .andExpect(jsonPath("$.origem.orcamento.id").value(origem.orcamentoId()))
                .andExpect(jsonPath("$.origem.versao.id").value(origem.versaoId()))
                .andExpect(jsonPath("$.origem.cliente.id").value(origem.clienteId()))
                .andExpect(jsonPath("$.acoesPermitidas.editarObservacao").value(true))
                .andExpect(jsonPath("$.acoesPermitidas.alterarStatusPara[0].codigo")
                        .value("EM_EXECUCAO"));

        mockMvc.perform(get("/ordens-servico")
                        .param("numero", Long.toString(numero))
                        .param("status", "COMPRAR_MATERIAL")
                        .param("clienteId", Long.toString(origem.clienteId()))
                        .param("orcamentoId", Long.toString(origem.orcamentoId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ordemId));

        mockMvc.perform(put("/ordens-servico/{id}", ordemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Separar materiais prioritários\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao")
                        .value("Separar materiais prioritários"));

        alterarStatusOperacional(ordemId, "EM_EXECUCAO");
        alterarStatusOperacional(ordemId, "INSTALAR");
        alterarStatusOperacional(ordemId, "CONCLUIDO");

        mockMvc.perform(get("/ordens-servico/{id}", ordemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.codigo").value("CONCLUIDO"))
                .andExpect(jsonPath("$.observacao")
                        .value("Separar materiais prioritários"))
                .andExpect(jsonPath("$.acoesPermitidas.editarObservacao").value(false))
                .andExpect(jsonPath("$.acoesPermitidas.alterarStatusPara").isEmpty());
    }

    @Test
    void deveExporAcoesDaVersaoSemReplicarARegraNoFrontend() throws Exception {
        Origem origem = criarOrcamento("Ações da versão");

        mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesPermitidas.editarConteudo").value(true))
                .andExpect(jsonPath("$.acoesPermitidas.criarNovaVersao").value(false))
                .andExpect(jsonPath("$.acoesPermitidas.alterarStatusPara[*].codigo")
                        .value(org.hamcrest.Matchers.contains("ENVIADO", "CANCELADO")));

        alterarStatusComercial(origem, "ENVIADO");

        mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesPermitidas.editarConteudo").value(false))
                .andExpect(jsonPath("$.acoesPermitidas.criarNovaVersao").value(true))
                .andExpect(jsonPath("$.acoesPermitidas.alterarStatusPara[*].codigo")
                        .value(org.hamcrest.Matchers.contains(
                                "APROVADO", "RECUSADO", "CANCELADO")));

        String novaBody = mockMvc.perform(post(
                        "/orcamentos/{orcamentoId}/versoes/{versaoId}/nova-versao",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acoesPermitidas.editarConteudo").value(true))
                .andReturn().getResponse().getContentAsString();
        long novaVersaoId = objectMapper.readTree(novaBody).get("id").asLong();

        mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesPermitidas.editarConteudo").value(false))
                .andExpect(jsonPath("$.acoesPermitidas.criarNovaVersao").value(false))
                .andExpect(jsonPath("$.acoesPermitidas.alterarStatusPara").isEmpty());

        mockMvc.perform(get("/orcamentos/{orcamentoId}/versoes/{versaoId}",
                        origem.orcamentoId(), novaVersaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesPermitidas.editarConteudo").value(true));
    }

    @Test
    void deveRejeitarCriacaoHttpParaVersaoNaoAprovada() throws Exception {
        Origem origem = criarOrcamento("Não aprovado");

        mockMvc.perform(post("/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail").value(
                        "A ordem de serviço só pode ser criada a partir de uma versão APROVADA."));
    }

    @Test
    void deveRejeitarSegundaOrdemPelaMesmaVersaoAprovada() throws Exception {
        Origem origem = criarOrcamento("Ordem duplicada");
        alterarStatusComercial(origem, "ENVIADO");
        alterarStatusComercial(origem, "APROVADO");
        criarOrdem(origem);

        mockMvc.perform(post("/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Já existe uma ordem de serviço para esta versão de orçamento."));
    }

    @Test
    void deveCongelarObservacaoAposConclusaoPeloContratoHttp() throws Exception {
        Origem origem = criarOrcamento("Concluída");
        alterarStatusComercial(origem, "ENVIADO");
        alterarStatusComercial(origem, "APROVADO");
        long ordemId = criarOrdem(origem).get("id").asLong();
        alterarStatusOperacional(ordemId, "EM_EXECUCAO");
        alterarStatusOperacional(ordemId, "INSTALAR");
        alterarStatusOperacional(ordemId, "CONCLUIDO");

        mockMvc.perform(put("/ordens-servico/{id}", ordemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Alteração proibida\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("A observação não pode ser alterada no status CONCLUIDO."));
    }

    private Origem criarOrcamento(String nome) throws Exception {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome(nome + " " + UUID.randomUUID()).build());
        String body = mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"observacao\":\"Proposta\"}"
                                .formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versaoAtual.status.codigo").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Origem(json.get("id").asLong(),
                json.get("versaoAtual").get("id").asLong(), cliente.getId());
    }

    private void alterarStatusComercial(Origem origem, String codigo) throws Exception {
        Long statusId = statusOrcamentoRepository.findByCodigo(codigo).orElseThrow().getId();
        mockMvc.perform(put("/orcamentos/{orcamentoId}/versoes/{versaoId}/status",
                        origem.orcamentoId(), origem.versaoId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrcamentoId\":%d}".formatted(statusId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.codigo").value(codigo));
    }

    private JsonNode criarOrdem(Origem origem) throws Exception {
        String body = mockMvc.perform(post(
                        "/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico",
                        origem.orcamentoId(), origem.versaoId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status.codigo").value("COMPRAR_MATERIAL"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void alterarStatusOperacional(long ordemId, String codigo) throws Exception {
        Long statusId = statusOrdemServicoRepository.findByCodigo(codigo).orElseThrow().getId();
        mockMvc.perform(put("/ordens-servico/{id}/status", ordemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrdemServicoId\":%d}".formatted(statusId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.codigo").value(codigo));
    }

    private record Origem(long orcamentoId, long versaoId, long clienteId) {
    }
}
