package br.com.nucleodasreformas.nucleoerp.categoria_servico.controller;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.service.CategoriaServicoService;
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

@WebMvcTest(CategoriaServicoController.class)
@Import(GlobalExceptionHandler.class)
class CategoriaServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaServicoService service;

    @Test
    void deveCadastrarCategoriaValida() throws Exception {
        when(service.salvar(any())).thenReturn(response(true));

        mockMvc.perform(post("/categorias-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pintura","ativo":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Pintura"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarNomeEmBranco() throws Exception {
        mockMvc.perform(post("/categorias-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarNomeAusente() throws Exception {
        mockMvc.perform(post("/categorias-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarNomeAcimaDeDuzentosCaracteres() throws Exception {
        mockMvc.perform(post("/categorias-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s"}
                                """.formatted("a".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarProblemDetailParaNomeDuplicado() throws Exception {
        when(service.salvar(any()))
                .thenThrow(new BusinessException("Já existe uma categoria de serviço com esse nome."));

        mockMvc.perform(post("/categorias-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pintura"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Já existe uma categoria de serviço com esse nome."));
    }

    @Test
    void deveBuscarCategoriaPorIdInclusiveInativa() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(response(false));

        mockMvc.perform(get("/categorias-servico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornarProblemDetailQuandoCategoriaNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Categoria de serviço não encontrada. Id: 99"));

        mockMvc.perform(get("/categorias-servico/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Categoria de serviço não encontrada. Id: 99"));
    }

    @Test
    void deveListarCategoriasAtivas() throws Exception {
        when(service.listar()).thenReturn(List.of(response(true)));

        mockMvc.perform(get("/categorias-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarCategoriaValida() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response(false));

        mockMvc.perform(put("/categorias-servico/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pintura","ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRejeitarPutComNomeEmBranco() throws Exception {
        mockMvc.perform(put("/categorias-servico/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirCategoriaLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/categorias-servico/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(1L);
    }

    private CategoriaServicoResponse response(boolean ativo) {
        return CategoriaServicoResponse.builder()
                .id(1L)
                .nome("Pintura")
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
