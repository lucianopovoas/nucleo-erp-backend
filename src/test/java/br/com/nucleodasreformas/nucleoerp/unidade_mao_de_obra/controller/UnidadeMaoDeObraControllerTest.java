package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.service.UnidadeMaoDeObraService;
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

@WebMvcTest(UnidadeMaoDeObraController.class)
@Import(GlobalExceptionHandler.class)
class UnidadeMaoDeObraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnidadeMaoDeObraService service;

    @Test
    void deveCadastrarUnidadeValidaSempreAtiva() throws Exception {
        when(service.salvar(any())).thenReturn(response(true));

        mockMvc.perform(post("/unidades-mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Hora","ativo":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Hora"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarNomeEmBranco() throws Exception {
        mockMvc.perform(post("/unidades-mao-de-obra")
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
        mockMvc.perform(post("/unidades-mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarNomeAcimaDeCemCaracteres() throws Exception {
        mockMvc.perform(post("/unidades-mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s"}
                                """.formatted("a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarProblemDetailParaNomeDuplicado() throws Exception {
        when(service.salvar(any()))
                .thenThrow(new BusinessException(
                        "Já existe uma unidade de mão de obra com esse nome."));

        mockMvc.perform(post("/unidades-mao-de-obra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Hora"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Já existe uma unidade de mão de obra com esse nome."));
    }

    @Test
    void deveBuscarUnidadePorIdInclusiveInativa() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(response(false));

        mockMvc.perform(get("/unidades-mao-de-obra/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornarProblemDetailQuandoUnidadeNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Unidade de mão de obra não encontrada. Id: 99"));

        mockMvc.perform(get("/unidades-mao-de-obra/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Unidade de mão de obra não encontrada. Id: 99"));
    }

    @Test
    void deveListarUnidadesAtivas() throws Exception {
        when(service.listar()).thenReturn(List.of(response(true)));

        mockMvc.perform(get("/unidades-mao-de-obra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarUnidadeValida() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response(false));

        mockMvc.perform(put("/unidades-mao-de-obra/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Hora","ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRejeitarPutComNomeEmBranco() throws Exception {
        mockMvc.perform(put("/unidades-mao-de-obra/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirUnidadeLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/unidades-mao-de-obra/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(1L);
    }

    private UnidadeMaoDeObraResponse response(boolean ativo) {
        return UnidadeMaoDeObraResponse.builder()
                .id(1L)
                .nome("Hora")
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
