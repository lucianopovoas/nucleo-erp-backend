package br.com.nucleodasreformas.nucleoerp.fornecedor.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.service.FornecedorService;
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

@WebMvcTest(FornecedorController.class)
@Import(GlobalExceptionHandler.class)
class FornecedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FornecedorService service;

    @Test
    void deveCadastrarFornecedorValidoComEmail() throws Exception {
        when(service.salvar(any())).thenReturn(response());

        mockMvc.perform(post("/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("contato@fornecedor.com"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarNomeEmBranco() throws Exception {
        mockMvc.perform(post("/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":" ","email":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarEmailComFormatoInvalido() throws Exception {
        mockMvc.perform(post("/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Fornecedor",
                                  "email": "email-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.email").value("O email deve possuir um formato válido."));

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarEmailAcimaDeCentoECinquentaCaracteres() throws Exception {
        String email = "a".repeat(145) + "@x.com";

        mockMvc.perform(post("/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Fornecedor",
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.email").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveBuscarFornecedorPorId() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(response());

        mockMvc.perform(get("/fornecedores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("contato@fornecedor.com"));
    }

    @Test
    void deveRetornarProblemDetailQuandoFornecedorNaoExistir() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Fornecedor não encontrado Id:99"));

        mockMvc.perform(get("/fornecedores/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Fornecedor não encontrado Id:99"));
    }

    @Test
    void deveListarFornecedores() throws Exception {
        when(service.listar()).thenReturn(List.of(response()));

        mockMvc.perform(get("/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deveAtualizarFornecedorValido() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response());

        mockMvc.perform(put("/fornecedores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("contato@fornecedor.com"));
    }

    @Test
    void deveRejeitarPutComEmailInvalido() throws Exception {
        mockMvc.perform(put("/fornecedores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Fornecedor","email":"invalido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirFornecedorLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/fornecedores/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(1L);
    }

    private String jsonValido() {
        return """
                {
                  "nome":"Fornecedor",
                  "endereco":"Rua A",
                  "celular":"71999998888",
                  "email":"contato@fornecedor.com",
                  "contato":"Contato A"
                }
                """;
    }

    private FornecedorResponse response() {
        return FornecedorResponse.builder().id(1L).nome("Fornecedor").endereco("Rua A")
                .celular("71999998888").email("contato@fornecedor.com").contato("Contato A")
                .ativo(true).criadoEm(LocalDateTime.of(2026, 8, 19, 12, 0)).build();
    }
}
