package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
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

@WebMvcTest(DespesaOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class DespesaOrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DespesaOrcamentoService service;

    @Test
    void deveCriarDespesaValida() throws Exception {
        when(service.salvar(eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "Frete",
                                  "valor": 180.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.descricao").value("Frete"))
                .andExpect(jsonPath("$.valor").value(180.0))
                .andExpect(jsonPath("$.criadoEm").exists());
    }

    @Test
    void devePermitirValorZeroNoPost() throws Exception {
        when(service.salvar(eq(10L), any())).thenReturn(responseComValor("0.00"));

        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "Cortesia",
                                  "valor": 0.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(0.0));
    }

    @Test
    void deveRejeitarCamposObrigatoriosAusentesNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.valor").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarDescricaoVaziaOuAcimaDoLimiteNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"   ","valor":10.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao").exists());

        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"%s","valor":10.00}
                                """.formatted("a".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao").exists());
    }

    @Test
    void deveRejeitarValorNegativoOuComMaisDeDuasCasasNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Frete","valor":-0.01}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());

        mockMvc.perform(post("/orcamentos/10/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Frete","valor":10.001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveListarDespesasDoOrcamento() throws Exception {
        when(service.listar(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/orcamentos/10/despesas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].descricao").value("Frete"));
    }

    @Test
    void deveBuscarDespesaPorOrcamentoEId() throws Exception {
        when(service.buscarPorId(10L, 20L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/despesas/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));

        verify(service).buscarPorId(10L, 20L);
    }

    @Test
    void deveRetornarProblemDetailQuandoDespesaNaoPertencerAoOrcamento() throws Exception {
        when(service.buscarPorId(99L, 20L)).thenThrow(new ResourceNotFoundException(
                "Despesa do orçamento não encontrada. Id: 20, orçamento: 99"));

        mockMvc.perform(get("/orcamentos/99/despesas/20"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void deveEncaminharAtualizacaoParcial() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "  Frete adicional  ",
                                  "valor": 50.00
                                }
                                """))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                DespesaOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("  Frete adicional  ");
        assertThat(captor.getValue().isDescricaoInformada()).isTrue();
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveDistinguirDescricaoOmitidaDeNullExplicitoNoPut() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                DespesaOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().isDescricaoInformada()).isFalse();

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAceitarValorNullNoPutParaPreservarAtual() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":null}
                                """))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                DespesaOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().getValor()).isNull();
    }

    @Test
    void deveRejeitarDescricaoVaziaValorNegativoOuEscalaInvalidaNoPut() throws Exception {
        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":-0.01}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());

        mockMvc.perform(put("/orcamentos/10/despesas/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":10.001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());
    }

    @Test
    void deveExcluirFisicamenteComNoContent() throws Exception {
        mockMvc.perform(delete("/orcamentos/10/despesas/20"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(10L, 20L);
    }

    private DespesaOrcamentoResponse response() {
        return responseComValor("180.00");
    }

    private DespesaOrcamentoResponse responseComValor(String valor) {
        return DespesaOrcamentoResponse.builder()
                .id(20L)
                .descricao("Frete")
                .valor(new BigDecimal(valor))
                .criadoEm(LocalDateTime.of(2026, 8, 21, 12, 0))
                .build();
    }
}
