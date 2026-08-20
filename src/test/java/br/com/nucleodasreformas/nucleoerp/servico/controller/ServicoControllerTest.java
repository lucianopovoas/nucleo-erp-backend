package br.com.nucleodasreformas.nucleoerp.servico.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.servico.dto.CategoriaServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.service.ServicoService;
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

@WebMvcTest(ServicoController.class)
@Import(GlobalExceptionHandler.class)
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoService service;

    @Test
    void deveCadastrarServicoValido() throws Exception {
        when(service.salvar(any())).thenReturn(response(true));

        mockMvc.perform(post("/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Instalação de toldo"))
                .andExpect(jsonPath("$.categoriaServico.id").value(3))
                .andExpect(jsonPath("$.categoriaServico.nome").value("Toldos"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarNomeEmBrancoECategoriaAusente() throws Exception {
        mockMvc.perform(post("/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.categoriaServicoId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarNomeAcimaDeDuzentosCaracteres() throws Exception {
        mockMvc.perform(post("/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","categoriaServicoId":3}
                                """.formatted("a".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarProblemDetailParaCategoriaInativa() throws Exception {
        when(service.salvar(any())).thenThrow(new BusinessException(
                "Não é possível vincular um serviço a uma categoria inativa."));

        mockMvc.perform(post("/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("Não é possível vincular um serviço a uma categoria inativa."));
    }

    @Test
    void deveBuscarServicoPorIdInclusiveInativo() throws Exception {
        when(service.buscarPorId(10L)).thenReturn(response(false));

        mockMvc.perform(get("/servicos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornarProblemDetailQuandoServicoNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Serviço não encontrado. Id: 99"));

        mockMvc.perform(get("/servicos/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Serviço não encontrado. Id: 99"));
    }

    @Test
    void deveListarServicosAtivos() throws Exception {
        when(service.listar()).thenReturn(List.of(response(true)));

        mockMvc.perform(get("/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarServicoValido() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response(false));

        mockMvc.perform(put("/servicos/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Instalação de toldo","categoriaServicoId":3,"ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRejeitarPutSemCategoria() throws Exception {
        mockMvc.perform(put("/servicos/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Instalação"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.categoriaServicoId").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirServicoLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(10L);

        mockMvc.perform(delete("/servicos/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(10L);
    }

    private String jsonValido() {
        return """
                {"nome":"Instalação de toldo","categoriaServicoId":3,"ativo":false}
                """;
    }

    private ServicoResponse response(boolean ativo) {
        return ServicoResponse.builder()
                .id(10L)
                .nome("Instalação de toldo")
                .categoriaServico(CategoriaServicoResumoResponse.builder().id(3L).nome("Toldos").build())
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
