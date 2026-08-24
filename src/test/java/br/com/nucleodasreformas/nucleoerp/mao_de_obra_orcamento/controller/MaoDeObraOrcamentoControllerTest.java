package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaoDeObraOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class MaoDeObraOrcamentoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MaoDeObraOrcamentoService service;

    @Test
    void deveCriarMaoDeObraComSnapshotEIdsDeContexto() throws Exception {
        when(service.salvar(any(), any(), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/versoes/20/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeMaoDeObraId":5,"descricao":"Instalação",
                                 "quantidade":2.0000,"custoUnitario":250.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.unidadeMaoDeObra.id").value(5))
                .andExpect(jsonPath("$.unidade").value("Hora"))
                .andExpect(jsonPath("$.custoTotal").value(500.00));

        var captor = org.mockito.ArgumentCaptor.forClass(MaoDeObraOrcamentoRequest.class);
        verify(service).salvar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getUnidadeMaoDeObraId()).isEqualTo(5L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("Instalação");
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("2.0000");
    }

    @Test
    void deveListarEBuscarMaoDeObraNaVersaoInformada() throws Exception {
        when(service.listar(10L, 20L)).thenReturn(List.of(response()));
        when(service.buscarPorId(10L, 20L, 30L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/versoes/20/mao-de-obra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
        mockMvc.perform(get("/orcamentos/10/versoes/20/mao-de-obra/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Instalação"));

        verify(service).listar(10L, 20L);
        verify(service).buscarPorId(10L, 20L, 30L);
    }

    @Test
    void deveAtualizarEExcluirMaoDeObraContextual() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20/mao-de-obra/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidadeMaoDeObraId\":6,\"descricao\":\"Montagem\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/orcamentos/10/versoes/20/mao-de-obra/30"))
                .andExpect(status().isNoContent());

        var captor = org.mockito.ArgumentCaptor.forClass(MaoDeObraOrcamentoUpdateRequest.class);
        verify(service).atualizar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(30L),
                captor.capture());
        assertThat(captor.getValue().getUnidadeMaoDeObraId()).isEqualTo(6L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("Montagem");
        verify(service).deletar(10L, 20L, 30L);
    }

    @Test
    void deveRetornarProblemDetailParaRequestInvalido() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\" \",\"quantidade\":0,\"custoUnitario\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.unidadeMaoDeObraId").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveTratarLinhaDeOutraVersaoComoInexistente() throws Exception {
        when(service.buscarPorId(10L, 20L, 99L)).thenThrow(
                new ResourceNotFoundException("Mão de obra do orçamento não encontrada. Id: 99"));

        mockMvc.perform(get("/orcamentos/10/versoes/20/mao-de-obra/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Mão de obra do orçamento não encontrada. Id: 99"));
    }

    @Test
    void deveRetornarProblemDetailAoEditarVersaoBloqueada() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenThrow(
                new BusinessException("A versão informada não está disponível para edição."));

        mockMvc.perform(put("/orcamentos/10/versoes/20/mao-de-obra/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":3.0000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("A versão informada não está disponível para edição."));
    }

    private MaoDeObraOrcamentoResponse response() {
        return MaoDeObraOrcamentoResponse.builder()
                .id(30L)
                .unidadeMaoDeObra(UnidadeMaoDeObraResumoResponse.builder()
                        .id(5L).nome("Hora").build())
                .descricao("Instalação")
                .unidade("Hora")
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("250.00"))
                .custoTotal(new BigDecimal("500.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 23, 11, 0))
                .build();
    }
}
