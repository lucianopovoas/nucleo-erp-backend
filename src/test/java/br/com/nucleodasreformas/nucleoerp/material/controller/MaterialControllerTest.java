package br.com.nucleodasreformas.nucleoerp.material.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.service.MaterialService;
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

@WebMvcTest(MaterialController.class)
@Import(GlobalExceptionHandler.class)
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService service;

    @Test
    void deveCadastrarMaterialValido() throws Exception {
        when(service.salvar(any())).thenReturn(response());

        mockMvc.perform(post("/materiais").contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Lona"))
                .andExpect(jsonPath("$.largura").value(1.50))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarPostComDescricaoAcimaDoLimite() throws Exception {
        String descricao = "a".repeat(101);

        mockMvc.perform(post("/materiais").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Lona","descricao":"%s","unidade":"M2"}
                        """.formatted(descricao)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.erros.descricao").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveBuscarMaterialPorId() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(response());

        mockMvc.perform(get("/materiais/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deveRetornarProblemDetailQuandoMaterialNaoExistir() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Material não encontrado Id:99"));

        mockMvc.perform(get("/materiais/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Material não encontrado Id:99"));
    }

    @Test
    void deveListarMateriais() throws Exception {
        when(service.listar()).thenReturn(List.of(response()));

        mockMvc.perform(get("/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deveAtualizarMaterialValido() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response());

        mockMvc.perform(put("/materiais/1").contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deveRejeitarPutComUnidadeAcimaDoLimite() throws Exception {
        mockMvc.perform(put("/materiais/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Lona","unidade":"12345678901"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.unidade").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirMaterialLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/materiais/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(1L);
    }

    private String jsonValido() {
        return """
                {"nome":"Lona","descricao":"Lona reforçada","unidade":"M2","largura":1.50}
                """;
    }

    private MaterialResponse response() {
        return MaterialResponse.builder().id(1L).nome("Lona").descricao("Lona reforçada")
                .unidade("M2").largura(new BigDecimal("1.50")).ativo(true)
                .criadoEm(LocalDateTime.of(2026, 8, 19, 12, 0)).build();
    }
}
