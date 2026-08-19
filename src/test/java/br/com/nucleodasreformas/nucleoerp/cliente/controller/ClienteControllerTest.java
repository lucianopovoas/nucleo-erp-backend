package br.com.nucleodasreformas.nucleoerp.cliente.controller;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.service.ClienteService;
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

@WebMvcTest(ClienteController.class)
@Import(GlobalExceptionHandler.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService service;

    @Test
    void deveCadastrarClienteValido() throws Exception {
        when(service.salvar(any())).thenReturn(response());

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Cliente A"))
                .andExpect(jsonPath("$.email").value("cliente@teste.com"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRejeitarPostComNomeEmBrancoEFormatoProblemDetail() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":" ","email":"cliente@teste.com"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.erros.nome").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarEmailInvalido() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Cliente A","email":"email-invalido"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveBuscarClientePorId() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(response());

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deveRetornarProblemDetailQuandoClienteNaoExistir() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Cliente não encontrado. Id: 99"));

        mockMvc.perform(get("/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado. Id: 99"));
    }

    @Test
    void deveListarClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(response()));

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deveAtualizarClienteValido() throws Exception {
        when(service.atualizar(any(), any())).thenReturn(response());

        mockMvc.perform(put("/clientes/1").contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deveRejeitarPutComCpfAcimaDoLimite() throws Exception {
        mockMvc.perform(put("/clientes/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Cliente A","cpf":"123456789012345"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cpf").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveExcluirClienteLogicamentePorDelegacao() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/clientes/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deletar(1L);
    }

    private String jsonValido() {
        return """
                {
                  "nome":"Cliente A",
                  "cpf":"12345678901",
                  "cnpj":"12345678000199",
                  "telefone":"7133334444",
                  "celular":"71999998888",
                  "email":"cliente@teste.com",
                  "contato":"Contato A",
                  "endereco":"Rua A"
                }
                """;
    }

    private ClienteResponse response() {
        return ClienteResponse.builder().id(1L).nome("Cliente A").cpf("12345678901")
                .cnpj("12345678000199").telefone("7133334444").celular("71999998888")
                .email("cliente@teste.com").contato("Contato A").endereco("Rua A")
                .ativo(true).criadoEm(LocalDateTime.of(2026, 8, 19, 12, 0)).build();
    }
}
