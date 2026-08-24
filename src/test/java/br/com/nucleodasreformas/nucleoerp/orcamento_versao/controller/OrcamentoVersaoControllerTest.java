package br.com.nucleodasreformas.nucleoerp.orcamento_versao.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrcamentoVersaoController.class)
@Import(GlobalExceptionHandler.class)
class OrcamentoVersaoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OrcamentoVersaoService service;

    @Test
    void deveListarEBuscarVersaoNoContextoDoOrcamento() throws Exception {
        when(service.listar(10L)).thenReturn(List.of(response()));
        when(service.buscarPorId(10L, 20L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/versoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].status.codigo").value("RASCUNHO"))
                .andExpect(jsonPath("$[0].totalComercial").value(1000.00))
                .andExpect(jsonPath("$[0].margemPrevista").value(400.00));

        mockMvc.perform(get("/orcamentos/10/versoes/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVersao").value(2))
                .andExpect(jsonPath("$.percentualMargem").value(40.00));

        verify(service).listar(10L);
        verify(service).buscarPorId(10L, 20L);
    }

    @Test
    void deveAtualizarObservacaoPreservandoIdsDeContexto() throws Exception {
        when(service.atualizar(any(), any(), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Proposta revisada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("Proposta revisada"));

        var captor = org.mockito.ArgumentCaptor.forClass(OrcamentoVersaoUpdateRequest.class);
        verify(service).atualizar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getObservacao()).isEqualTo("Proposta revisada");
        assertThat(captor.getValue().isObservacaoInformada()).isTrue();
    }

    @Test
    void deveAlterarStatusECriarNovaVersaoPelasRotasEspecificas() throws Exception {
        when(service.alterarStatus(any(), any(), any())).thenReturn(response());
        when(service.criarNovaVersao(10L, 20L)).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrcamentoId\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/orcamentos/10/versoes/20/nova-versao"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));

        var captor = org.mockito.ArgumentCaptor.forClass(OrcamentoVersaoStatusRequest.class);
        verify(service).alterarStatus(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getStatusOrcamentoId()).isEqualTo(2L);
        verify(service).criarNovaVersao(10L, 20L);
    }

    @Test
    void deveRetornarProblemDetailParaStatusObrigatorio() throws Exception {
        mockMvc.perform(put("/orcamentos/10/versoes/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos estão inválidos."))
                .andExpect(jsonPath("$.erros.statusOrcamentoId")
                        .value("O status é obrigatório."));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarProblemDetailQuandoVersaoEstiverBloqueada() throws Exception {
        when(service.atualizar(any(), any(), any())).thenThrow(
                new BusinessException("Somente a versão atual em RASCUNHO pode ser alterada."));

        mockMvc.perform(put("/orcamentos/10/versoes/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Bloqueada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Somente a versão atual em RASCUNHO pode ser alterada."));
    }

    @Test
    void deveTratarVersaoDeOutroOrcamentoComoInexistente() throws Exception {
        when(service.buscarPorId(10L, 99L)).thenThrow(
                new ResourceNotFoundException("Versão do orçamento não encontrada. Id: 99"));

        mockMvc.perform(get("/orcamentos/10/versoes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Versão do orçamento não encontrada. Id: 99"));
    }

    @Test
    void deveRejeitarJsonMalformadoSemFixarContratoAindaNaoPadronizado() throws Exception {
        mockMvc.perform(put("/orcamentos/10/versoes/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrcamentoId\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarPathVariableInvalidaSemFixarContratoAindaNaoPadronizado() throws Exception {
        mockMvc.perform(get("/orcamentos/invalido/versoes/20"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private OrcamentoVersaoResponse response() {
        return OrcamentoVersaoResponse.builder()
                .id(20L)
                .numeroVersao(2)
                .status(StatusOrcamentoResumoResponse.builder()
                        .id(1L).codigo("RASCUNHO").nome("Rascunho").build())
                .observacao("Proposta revisada")
                .totalComercial(new BigDecimal("1000.00"))
                .custoTotalMateriais(new BigDecimal("300.00"))
                .custoTotalMaoDeObra(new BigDecimal("200.00"))
                .custoTotalDespesas(new BigDecimal("100.00"))
                .margemPrevista(new BigDecimal("400.00"))
                .percentualMargem(new BigDecimal("40.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 23, 10, 0))
                .build();
    }
}
