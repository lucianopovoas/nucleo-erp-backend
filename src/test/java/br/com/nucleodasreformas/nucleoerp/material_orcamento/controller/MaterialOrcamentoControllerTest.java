package br.com.nucleodasreformas.nucleoerp.material_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialResumoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
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

@WebMvcTest(MaterialOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class MaterialOrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialOrcamentoService service;

    @Test
    void deveCriarMaterialPrevistoNoOrcamento() throws Exception {
        when(service.salvar(eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":5,"quantidade":2.5,"custoUnitario":75.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.material.id").value(5))
                .andExpect(jsonPath("$.material.nome").value("Lona"))
                .andExpect(jsonPath("$.descricao").value("Lona"))
                .andExpect(jsonPath("$.unidade").value("M2"))
                .andExpect(jsonPath("$.quantidade").value(2.5))
                .andExpect(jsonPath("$.custoUnitario").value(75.0))
                .andExpect(jsonPath("$.custoTotal").value(187.5));
    }

    @Test
    void deveRejeitarPostSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/orcamentos/10/materiais")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.materialId").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarValoresInvalidos() throws Exception {
        mockMvc.perform(post("/orcamentos/10/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":5,"quantidade":0,"custoUnitario":-0.01}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveListarMateriaisDoOrcamento() throws Exception {
        when(service.listar(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/orcamentos/10/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void deveBuscarMaterialPelosIdentificadoresAninhados() throws Exception {
        when(service.buscarPorId(10L, 20L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/materiais/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));

        verify(service).buscarPorId(10L, 20L);
    }

    @Test
    void deveRetornarNotFoundQuandoLinhaNaoPertencerAoOrcamento() throws Exception {
        when(service.buscarPorId(10L, 99L)).thenThrow(new ResourceNotFoundException(
                "Material do orçamento não encontrado. Id: 99, orçamento: 10"));

        mockMvc.perform(get("/orcamentos/10/materiais/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("Material do orçamento não encontrado. Id: 99, orçamento: 10"));
    }

    @Test
    void deveEncaminharAtualizacaoParcialSemUnidadeOuCustoTotal() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/materiais/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":6,"descricao":"  Lona frontal  ","quantidade":3}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<MaterialOrcamentoUpdateRequest> captor =
                ArgumentCaptor.forClass(MaterialOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().getMaterialId()).isEqualTo(6L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("  Lona frontal  ");
        assertThat(captor.getValue().isDescricaoInformada()).isTrue();
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("3");
    }

    @Test
    void deveDistinguirDescricaoOmitidaDeNullExplicito() throws Exception {
        when(service.atualizar(eq(10L), eq(20L), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/materiais/20")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        ArgumentCaptor<MaterialOrcamentoUpdateRequest> captor =
                ArgumentCaptor.forClass(MaterialOrcamentoUpdateRequest.class);
        verify(service).atualizar(eq(10L), eq(20L), captor.capture());
        assertThat(captor.getValue().isDescricaoInformada()).isFalse();

        mockMvc.perform(put("/orcamentos/10/materiais/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDescricaoVazia() throws Exception {
        mockMvc.perform(put("/orcamentos/10/materiais/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExcluirFisicamenteComNoContent() throws Exception {
        mockMvc.perform(delete("/orcamentos/10/materiais/20"))
                .andExpect(status().isNoContent());

        verify(service).deletar(10L, 20L);
    }

    private MaterialOrcamentoResponse response() {
        return MaterialOrcamentoResponse.builder()
                .id(20L)
                .material(MaterialResumoResponse.builder().id(5L).nome("Lona").build())
                .descricao("Lona").unidade("M2")
                .quantidade(new BigDecimal("2.5000"))
                .custoUnitario(new BigDecimal("75.00"))
                .custoTotal(new BigDecimal("187.50"))
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
