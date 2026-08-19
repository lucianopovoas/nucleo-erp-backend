package br.com.nucleodasreformas.nucleoerp.materialfornecedor.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.FornecedorResumoResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialResumoResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.service.MaterialFornecedorService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

@WebMvcTest(MaterialFornecedorController.class)
@Import(GlobalExceptionHandler.class)
class MaterialFornecedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialFornecedorService service;

    @Test
    void deveCadastrarVinculoValido() throws Exception {
        when(service.salvar(any())).thenReturn(response());

        mockMvc.perform(post("/materiais-fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "materialId": 1,
                                  "fornecedorId": 2,
                                  "precoCompra": 125.50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.material.id").value(1))
                .andExpect(jsonPath("$.material.nome").value("Lona"))
                .andExpect(jsonPath("$.fornecedor.id").value(2))
                .andExpect(jsonPath("$.fornecedor.nome").value("Fornecedor X"))
                .andExpect(jsonPath("$.precoCompra").value(125.50))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarPostInvalidoComProblemDetail() throws Exception {
        mockMvc.perform(post("/materiais-fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precoCompra": -1.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.materialId").exists())
                .andExpect(jsonPath("$.erros.fornecedorId").exists())
                .andExpect(jsonPath("$.erros.precoCompra").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveBuscarVinculoPorId() throws Exception {
        when(service.buscarPorId(10L)).thenReturn(response());

        mockMvc.perform(get("/materiais-fornecedores/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deveRetornarProblemDetailQuandoVinculoNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Vínculo entre material e fornecedor não encontrado. Id: 99"));

        mockMvc.perform(get("/materiais-fornecedores/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Vínculo entre material e fornecedor não encontrado. Id: 99"));
    }

    @Test
    void deveListarVinculosAtivos() throws Exception {
        when(service.listar()).thenReturn(List.of(response()));

        mockMvc.perform(get("/materiais-fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarVinculoValido() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response());

        mockMvc.perform(put("/materiais-fornecedores/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "materialId": 1,
                                  "fornecedorId": 2,
                                  "precoCompra": 125.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deveExcluirVinculoLogicamente() throws Exception {
        doNothing().when(service).deletar(10L);

        mockMvc.perform(delete("/materiais-fornecedores/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(10L);
    }

    private MaterialFornecedorResponse response() {
        return MaterialFornecedorResponse.builder()
                .id(10L)
                .material(MaterialResumoResponse.builder().id(1L).nome("Lona").build())
                .fornecedor(FornecedorResumoResponse.builder().id(2L).nome("Fornecedor X").build())
                .precoCompra(new BigDecimal("125.50"))
                .ativo(true)
                .criadoEm(LocalDateTime.of(2026, 8, 19, 12, 0))
                .build();
    }
}
