package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DespesaOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class DespesaOrcamentoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DespesaOrcamentoService service;

    @Test
    void deveCriarDespesaComJsonEIdsDeContexto() throws Exception {
        when(service.salvar(any(), any(), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/versoes/20/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Frete\",\"valor\":180.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.descricao").value("Frete"))
                .andExpect(jsonPath("$.valor").value(180.00));

        var captor = org.mockito.ArgumentCaptor.forClass(DespesaOrcamentoRequest.class);
        verify(service).salvar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Frete");
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("180.00");
    }

    @Test
    void deveListarEBuscarDespesaNaVersaoInformada() throws Exception {
        when(service.listar(10L, 20L)).thenReturn(List.of(response()));
        when(service.buscarPorId(10L, 20L, 30L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/versoes/20/despesas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
        mockMvc.perform(get("/orcamentos/10/versoes/20/despesas/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(180.00));

        verify(service).listar(10L, 20L);
        verify(service).buscarPorId(10L, 20L, 30L);
    }

    @Test
    void deveAtualizarEExcluirDespesaContextual() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20/despesas/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Frete adicional\",\"valor\":50.00}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/orcamentos/10/versoes/20/despesas/30"))
                .andExpect(status().isNoContent());

        var captor = org.mockito.ArgumentCaptor.forClass(DespesaOrcamentoUpdateRequest.class);
        verify(service).atualizar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(30L),
                captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Frete adicional");
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("50.00");
        verify(service).deletar(10L, 20L, 30L);
    }

    @Test
    void deveRetornarProblemDetailParaRequestInvalido() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\" \",\"valor\":-0.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.valor").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarValorComMaisDeDuasCasas() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/despesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Frete\",\"valor\":10.001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveTratarDespesaDeOutraVersaoComoInexistente() throws Exception {
        when(service.buscarPorId(10L, 20L, 99L)).thenThrow(
                new ResourceNotFoundException("Despesa do orçamento não encontrada. Id: 99"));

        mockMvc.perform(get("/orcamentos/10/versoes/20/despesas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Despesa do orçamento não encontrada. Id: 99"));
    }

    @Test
    void deveRetornarProblemDetailAoEditarVersaoBloqueada() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenThrow(
                new BusinessException("A versão informada não está disponível para edição."));

        mockMvc.perform(put("/orcamentos/10/versoes/20/despesas/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("A versão informada não está disponível para edição."));
    }

    private DespesaOrcamentoResponse response() {
        return DespesaOrcamentoResponse.builder()
                .id(30L)
                .descricao("Frete")
                .valor(new BigDecimal("180.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 23, 11, 0))
                .build();
    }
}
