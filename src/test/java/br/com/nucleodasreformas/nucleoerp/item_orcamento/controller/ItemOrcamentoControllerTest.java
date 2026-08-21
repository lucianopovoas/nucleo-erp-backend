package br.com.nucleodasreformas.nucleoerp.item_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
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

@WebMvcTest(ItemOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class ItemOrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemOrcamentoService service;

    @Test
    void deveCriarItemValidoNaRotaDoOrcamento() throws Exception {
        when(service.salvar(eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "servicoId": 5,
                                  "quantidade": 2.5,
                                  "valorUnitario": 150.00,
                                  "desconto": 20.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.servico.id").value(5))
                .andExpect(jsonPath("$.servico.nome").value("Instalação"))
                .andExpect(jsonPath("$.descricao").value("Instalação"))
                .andExpect(jsonPath("$.quantidade").value(2.5))
                .andExpect(jsonPath("$.valorUnitario").value(150.0))
                .andExpect(jsonPath("$.desconto").value(20.0))
                .andExpect(jsonPath("$.valorTotal").value(355.0));
    }

    @Test
    void deveRejeitarPostSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/orcamentos/10/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.servicoId").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.valorUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarValoresInvalidosNoPost() throws Exception {
        mockMvc.perform(post("/orcamentos/10/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicoId":5,"quantidade":0,"valorUnitario":-1,"desconto":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.valorUnitario").exists())
                .andExpect(jsonPath("$.erros.desconto").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveListarItensDoOrcamento() throws Exception {
        when(service.listar(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/orcamentos/10/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void deveBuscarItemPelosDoisIdentificadores() throws Exception {
        when(service.buscarPorId(10L, 20L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/itens/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));

        verify(service).buscarPorId(10L, 20L);
    }

    @Test
    void deveRetornarNotFoundQuandoItemNaoPertencerAoOrcamento() throws Exception {
        when(service.buscarPorId(10L, 99L)).thenThrow(new ResourceNotFoundException(
                "Item de orçamento não encontrado. Id: 99, orçamento: 10"));

        mockMvc.perform(get("/orcamentos/10/itens/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("Item de orçamento não encontrado. Id: 99, orçamento: 10"));
    }

    @Test
    void deveAtualizarItemERegistrarDescricaoExplicita() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/itens/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicoId":6,"descricao":"  Instalação frontal  ","quantidade":3}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<ItemOrcamentoUpdateRequest> captor =
                ArgumentCaptor.forClass(ItemOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().getServicoId()).isEqualTo(6L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("  Instalação frontal  ");
        assertThat(captor.getValue().isDescricaoInformada()).isTrue();
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("3");
    }

    @Test
    void deveDistinguirDescricaoOmitidaDeNullExplicito() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/itens/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ItemOrcamentoUpdateRequest> captor =
                ArgumentCaptor.forClass(ItemOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().isDescricaoInformada()).isFalse();

        mockMvc.perform(put("/orcamentos/10/itens/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDescricaoComSomenteEspacos() throws Exception {
        mockMvc.perform(put("/orcamentos/10/itens/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveTraduzirErroDeDescontoMaiorQueSubtotal() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any()))
                .thenThrow(new BusinessException("O desconto não pode ser maior que o subtotal do item."));

        mockMvc.perform(put("/orcamentos/10/itens/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"desconto":999.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"));
    }

    @Test
    void deveExcluirItemFisicamente() throws Exception {
        mockMvc.perform(delete("/orcamentos/10/itens/20"))
                .andExpect(status().isNoContent());

        verify(service).deletar(10L, 20L);
    }

    private ItemOrcamentoResponse response() {
        return ItemOrcamentoResponse.builder()
                .id(20L)
                .servico(ServicoResumoResponse.builder().id(5L).nome("Instalação").build())
                .descricao("Instalação")
                .quantidade(new BigDecimal("2.5000"))
                .valorUnitario(new BigDecimal("150.00"))
                .desconto(new BigDecimal("20.00"))
                .valorTotal(new BigDecimal("355.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
