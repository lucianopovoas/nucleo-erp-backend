package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.UnidadeMaoDeObraResumoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service.MaoDeObraOrcamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaoDeObraOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class MaoDeObraOrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaoDeObraOrcamentoService service;

    @Test
    void deveCriarMaoDeObraValida() throws Exception {
        when(service.salvar(eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unidadeMaoDeObraId": 5,
                                  "descricao": "Instalação",
                                  "quantidade": 2.0000,
                                  "custoUnitario": 250.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.unidadeMaoDeObra.id").value(5))
                .andExpect(jsonPath("$.unidadeMaoDeObra.nome").value("Nome atual"))
                .andExpect(jsonPath("$.descricao").value("Instalação"))
                .andExpect(jsonPath("$.unidade").value("Diária"))
                .andExpect(jsonPath("$.quantidade").value(2.0))
                .andExpect(jsonPath("$.custoUnitario").value(250.0))
                .andExpect(jsonPath("$.custoTotal").value(500.0))
                .andExpect(jsonPath("$.criadoEm").exists());
    }

    @Test
    void deveRejeitarCamposObrigatoriosAusentesNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.unidadeMaoDeObraId").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarDescricaoVaziaOuAcimaDoLimiteNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unidadeMaoDeObraId": 5,
                                  "descricao": "   ",
                                  "quantidade": 1,
                                  "custoUnitario": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao").exists());

        mockMvc.perform(post("/orcamentos/10/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unidadeMaoDeObraId": 5,
                                  "descricao": "%s",
                                  "quantidade": 1,
                                  "custoUnitario": 10
                                }
                                """.formatted("a".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao").exists());
    }

    @Test
    void deveRejeitarQuantidadeECustoInvalidosNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unidadeMaoDeObraId": 5,
                                  "descricao": "Instalação",
                                  "quantidade": 0,
                                  "custoUnitario": -0.01
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());
    }

    @Test
    void deveListarMaoDeObraDoOrcamento() throws Exception {
        when(service.listar(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/orcamentos/10/mao-de-obra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].unidade").value("Diária"));
    }

    @Test
    void deveBuscarMaoDeObraPorOrcamentoEId() throws Exception {
        when(service.buscarPorId(10L, 20L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/mao-de-obra/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));

        verify(service).buscarPorId(10L, 20L);
    }

    @Test
    void deveRetornarProblemDetailQuandoLinhaNaoPertencerAoOrcamento() throws Exception {
        when(service.buscarPorId(99L, 20L)).thenThrow(new ResourceNotFoundException(
                "Mão de obra do orçamento não encontrada. Id: 20, orçamento: 99"));

        mockMvc.perform(get("/orcamentos/99/mao-de-obra/20"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void deveEncaminharAtualizacaoParcial() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/mao-de-obra/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unidadeMaoDeObraId": 6,
                                  "descricao": "  Instalação principal  ",
                                  "quantidade": 3.0000,
                                  "custoUnitario": 125.00
                                }
                                """))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                MaoDeObraOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().getUnidadeMaoDeObraId()).isEqualTo(6L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("  Instalação principal  ");
        assertThat(captor.getValue().isDescricaoInformada()).isTrue();
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("3.0000");
        assertThat(captor.getValue().getCustoUnitario()).isEqualByComparingTo("125.00");
    }

    @Test
    void deveDistinguirDescricaoOmitidaDeNullExplicitoNoPut() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/mao-de-obra/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                MaoDeObraOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().isDescricaoInformada()).isFalse();

        mockMvc.perform(put("/orcamentos/10/mao-de-obra/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDescricaoVaziaNoPut() throws Exception {
        mockMvc.perform(put("/orcamentos/10/mao-de-obra/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExcluirFisicamenteComNoContent() throws Exception {
        mockMvc.perform(delete("/orcamentos/10/mao-de-obra/20"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(10L, 20L);
    }

    private MaoDeObraOrcamentoResponse response() {
        return MaoDeObraOrcamentoResponse.builder()
                .id(20L)
                .unidadeMaoDeObra(UnidadeMaoDeObraResumoResponse.builder()
                        .id(5L)
                        .nome("Nome atual")
                        .build())
                .descricao("Instalação")
                .unidade("Diária")
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("250.00"))
                .custoTotal(new BigDecimal("500.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 21, 12, 0))
                .build();
    }
}
