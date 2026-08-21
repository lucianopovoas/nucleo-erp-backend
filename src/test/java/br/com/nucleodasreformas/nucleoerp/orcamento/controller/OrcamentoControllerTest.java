package br.com.nucleodasreformas.nucleoerp.orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(OrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class OrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrcamentoService service;

    @Test
    void deveCadastrarOrcamentoValido() throws Exception {
        when(service.salvar(any())).thenReturn(response(
                "Rascunho",
                "Área externa",
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00")));

        mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":10,"observacao":"Área externa"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.numero").value(1234))
                .andExpect(jsonPath("$.cliente.id").value(10))
                .andExpect(jsonPath("$.cliente.nome").value("Cliente X"))
                .andExpect(jsonPath("$.status.nome").value("Rascunho"))
                .andExpect(jsonPath("$.observacao").value("Área externa"))
                .andExpect(jsonPath("$.totalComercial").value(0.00))
                .andExpect(jsonPath("$.custoTotalMateriais").value(0.00))
                .andExpect(jsonPath("$.custoTotalMaoDeObra").value(0.00));
    }

    @Test
    void deveRejeitarPostSemCliente() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observacao":"Sem cliente"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.clienteId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarErroDeNegocioQuandoStatusInicialNaoEstiverDisponivel() throws Exception {
        when(service.salvar(any()))
                .thenThrow(new BusinessException("O status inicial 'Rascunho' está inativo."));

        mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail").value("O status inicial 'Rascunho' está inativo."));
    }

    @Test
    void deveBuscarOrcamentoPorId() throws Exception {
        when(service.buscarPorId(5L)).thenReturn(response("Enviado", null));

        mockMvc.perform(get("/orcamentos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status.nome").value("Enviado"))
                .andExpect(jsonPath("$.totalComercial").value(350.00))
                .andExpect(jsonPath("$.custoTotalMateriais").value(180.00))
                .andExpect(jsonPath("$.custoTotalMaoDeObra").value(95.00));
    }

    @Test
    void deveRetornarProblemDetailQuandoOrcamentoNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Orçamento não encontrado. Id: 99"));

        mockMvc.perform(get("/orcamentos/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Orçamento não encontrado. Id: 99"));
    }

    @Test
    void deveListarOrcamentosInclusiveCancelados() throws Exception {
        when(service.listar()).thenReturn(List.of(response("Cancelado", null)));

        mockMvc.perform(get("/orcamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(1234))
                .andExpect(jsonPath("$[0].status.nome").value("Cancelado"))
                .andExpect(jsonPath("$[0].totalComercial").value(350.00))
                .andExpect(jsonPath("$[0].custoTotalMateriais").value(180.00))
                .andExpect(jsonPath("$[0].custoTotalMaoDeObra").value(95.00));
    }

    @Test
    void deveAtualizarSomenteObservacao() throws Exception {
        when(service.atualizar(eq(5L), any())).thenReturn(response("Rascunho", "Revisado"));

        mockMvc.perform(put("/orcamentos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"observacao":"Revisado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("Revisado"))
                .andExpect(jsonPath("$.totalComercial").value(350.00))
                .andExpect(jsonPath("$.custoTotalMateriais").value(180.00))
                .andExpect(jsonPath("$.custoTotalMaoDeObra").value(95.00));

        ArgumentCaptor<OrcamentoUpdateRequest> captor = ArgumentCaptor.forClass(OrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(5L), captor.capture());
        assertThat(captor.getValue().isObservacaoInformada()).isTrue();
        assertThat(captor.getValue().getClienteId()).isNull();
        assertThat(captor.getValue().getStatusOrcamentoId()).isNull();
    }

    @Test
    void deveDistinguirObservacaoOmitidaDeNullExplicito() throws Exception {
        when(service.atualizar(eq(5L), any())).thenReturn(response("Rascunho", null));

        mockMvc.perform(put("/orcamentos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/orcamentos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observacao":null}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<OrcamentoUpdateRequest> captor = ArgumentCaptor.forClass(OrcamentoUpdateRequest.class);
        verify(service, org.mockito.Mockito.times(2)).atualizar(eq(5L), captor.capture());
        assertThat(captor.getAllValues().get(0).isObservacaoInformada()).isFalse();
        assertThat(captor.getAllValues().get(1).isObservacaoInformada()).isTrue();
        assertThat(captor.getAllValues().get(1).getObservacao()).isNull();
    }

    @Test
    void naoDeveDisponibilizarEndpointDelete() throws Exception {
        mockMvc.perform(delete("/orcamentos/5"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(service);
    }

    private OrcamentoResponse response(String status, String observacao) {
        return response(
                status,
                observacao,
                new BigDecimal("350.00"),
                new BigDecimal("180.00"),
                new BigDecimal("95.00"));
    }

    private OrcamentoResponse response(
            String status,
            String observacao,
            BigDecimal totalComercial,
            BigDecimal custoTotalMateriais,
            BigDecimal custoTotalMaoDeObra) {
        return OrcamentoResponse.builder()
                .id(5L)
                .numero(1234L)
                .cliente(ClienteResumoResponse.builder().id(10L).nome("Cliente X").build())
                .status(StatusOrcamentoResumoResponse.builder().id(1L).nome(status).build())
                .observacao(observacao)
                .totalComercial(totalComercial)
                .custoTotalMateriais(custoTotalMateriais)
                .custoTotalMaoDeObra(custoTotalMaoDeObra)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
