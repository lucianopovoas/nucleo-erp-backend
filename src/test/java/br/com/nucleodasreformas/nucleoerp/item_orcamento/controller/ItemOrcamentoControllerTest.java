package br.com.nucleodasreformas.nucleoerp.item_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
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

@WebMvcTest(ItemOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class ItemOrcamentoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ItemOrcamentoService service;

    @Test
    void deveCriarItemComJsonMonetarioEIdsDeContexto() throws Exception {
        when(service.salvar(any(), any(), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/versoes/20/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicoId":5,"quantidade":2.5000,
                                 "valorUnitario":150.00,"desconto":20.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.servico.id").value(5))
                .andExpect(jsonPath("$.descricao").value("Instalação negociada"))
                .andExpect(jsonPath("$.quantidade").value(2.5000))
                .andExpect(jsonPath("$.valorUnitario").value(150.00))
                .andExpect(jsonPath("$.desconto").value(20.00))
                .andExpect(jsonPath("$.valorTotal").value(355.00));

        var captor = org.mockito.ArgumentCaptor.forClass(ItemOrcamentoRequest.class);
        verify(service).salvar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getServicoId()).isEqualTo(5L);
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(captor.getValue().getValorUnitario()).isEqualByComparingTo("150.00");
        assertThat(captor.getValue().getDesconto()).isEqualByComparingTo("20.00");
    }

    @Test
    void deveListarEBuscarItemNaVersaoInformada() throws Exception {
        when(service.listar(10L, 20L)).thenReturn(List.of(response()));
        when(service.buscarPorId(10L, 20L, 30L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/versoes/20/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
        mockMvc.perform(get("/orcamentos/10/versoes/20/itens/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(355.00));

        verify(service).listar(10L, 20L);
        verify(service).buscarPorId(10L, 20L, 30L);
    }

    @Test
    void deveAtualizarEExcluirSomenteALinhaContextual() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20/itens/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Revisada\",\"quantidade\":3.0000}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/orcamentos/10/versoes/20/itens/30"))
                .andExpect(status().isNoContent());

        var captor = org.mockito.ArgumentCaptor.forClass(ItemOrcamentoUpdateRequest.class);
        verify(service).atualizar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(30L),
                captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Revisada");
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("3.0000");
        verify(service).deletar(10L, 20L, 30L);
    }

    @Test
    void deveRetornarProblemDetailParaRequestInvalido() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":0,\"valorUnitario\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.servicoId").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.valorUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarEscalaMonetariaAcimaDoContratoHttp() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicoId":5,"quantidade":1.0000,
                                 "valorUnitario":10.001,"desconto":0.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valorUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveTratarLinhaDeOutraVersaoComoInexistente() throws Exception {
        when(service.buscarPorId(10L, 20L, 99L)).thenThrow(
                new ResourceNotFoundException("Item do orçamento não encontrado. Id: 99"));

        mockMvc.perform(get("/orcamentos/10/versoes/20/itens/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Item do orçamento não encontrado. Id: 99"));
    }

    @Test
    void deveRetornarErroDeNegocioAoEditarVersaoBloqueada() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenThrow(
                new BusinessException("Somente a versão atual em RASCUNHO pode ser alterada."));

        mockMvc.perform(put("/orcamentos/10/versoes/20/itens/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":3.0000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Somente a versão atual em RASCUNHO pode ser alterada."));
    }

    private ItemOrcamentoResponse response() {
        return ItemOrcamentoResponse.builder()
                .id(30L)
                .servico(ServicoResumoResponse.builder().id(5L).nome("Instalação").build())
                .descricao("Instalação negociada")
                .quantidade(new BigDecimal("2.5000"))
                .valorUnitario(new BigDecimal("150.00"))
                .desconto(new BigDecimal("20.00"))
                .valorTotal(new BigDecimal("355.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 23, 11, 0))
                .build();
    }
}
