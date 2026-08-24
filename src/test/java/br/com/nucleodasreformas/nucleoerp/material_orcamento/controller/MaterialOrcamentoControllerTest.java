package br.com.nucleodasreformas.nucleoerp.material_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialResumoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
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

@WebMvcTest(MaterialOrcamentoController.class)
@Import(GlobalExceptionHandler.class)
class MaterialOrcamentoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MaterialOrcamentoService service;

    @Test
    void deveCriarMaterialComSnapshotNoResponseEContextoCorreto() throws Exception {
        when(service.salvar(any(), any(), any())).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/versoes/20/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":5,\"quantidade\":2.5000,\"custoUnitario\":75.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.material.id").value(5))
                .andExpect(jsonPath("$.descricao").value("Lona negociada"))
                .andExpect(jsonPath("$.unidade").value("M2"))
                .andExpect(jsonPath("$.custoTotal").value(187.50));

        var captor = org.mockito.ArgumentCaptor.forClass(MaterialOrcamentoRequest.class);
        verify(service).salvar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().getMaterialId()).isEqualTo(5L);
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(captor.getValue().getCustoUnitario()).isEqualByComparingTo("75.00");
    }

    @Test
    void deveListarEBuscarMaterialNaVersaoInformada() throws Exception {
        when(service.listar(10L, 20L)).thenReturn(List.of(response()));
        when(service.buscarPorId(10L, 20L, 30L)).thenReturn(response());

        mockMvc.perform(get("/orcamentos/10/versoes/20/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
        mockMvc.perform(get("/orcamentos/10/versoes/20/materiais/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidade").value("M2"));

        verify(service).listar(10L, 20L);
        verify(service).buscarPorId(10L, 20L, 30L);
    }

    @Test
    void deveAtualizarEExcluirMaterialComTodosOsIds() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenReturn(response());

        mockMvc.perform(put("/orcamentos/10/versoes/20/materiais/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":6,\"descricao\":\"Revisada\",\"quantidade\":3.0000}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/orcamentos/10/versoes/20/materiais/30"))
                .andExpect(status().isNoContent());

        var captor = org.mockito.ArgumentCaptor.forClass(MaterialOrcamentoUpdateRequest.class);
        verify(service).atualizar(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(30L),
                captor.capture());
        assertThat(captor.getValue().getMaterialId()).isEqualTo(6L);
        assertThat(captor.getValue().getDescricao()).isEqualTo("Revisada");
        verify(service).deletar(10L, 20L, 30L);
    }

    @Test
    void deveRetornarProblemDetailParaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/orcamentos/10/versoes/20/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":0,\"custoUnitario\":-0.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.materialId").exists())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros.custoUnitario").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveTratarMaterialDeOutraVersaoComoInexistente() throws Exception {
        when(service.buscarPorId(10L, 20L, 99L)).thenThrow(
                new ResourceNotFoundException("Material do orçamento não encontrado. Id: 99"));

        mockMvc.perform(get("/orcamentos/10/versoes/20/materiais/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Material do orçamento não encontrado. Id: 99"));
    }

    @Test
    void deveRetornarProblemDetailAoEditarVersaoBloqueada() throws Exception {
        when(service.atualizar(any(), any(), any(), any())).thenThrow(
                new BusinessException("A versão informada não está disponível para edição."));

        mockMvc.perform(put("/orcamentos/10/versoes/20/materiais/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":3.0000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("A versão informada não está disponível para edição."));
    }

    private MaterialOrcamentoResponse response() {
        return MaterialOrcamentoResponse.builder()
                .id(30L)
                .material(MaterialResumoResponse.builder().id(5L).nome("Lona").build())
                .descricao("Lona negociada")
                .unidade("M2")
                .quantidade(new BigDecimal("2.5000"))
                .custoUnitario(new BigDecimal("75.00"))
                .custoTotal(new BigDecimal("187.50"))
                .criadoEm(LocalDateTime.of(2026, 8, 23, 11, 0))
                .build();
    }
}
