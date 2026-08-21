package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.service.StatusOrdemServicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

@WebMvcTest(StatusOrdemServicoController.class)
@Import(GlobalExceptionHandler.class)
class StatusOrdemServicoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private StatusOrdemServicoService service;

    @Test
    void deveExporCrudAdministrativo() throws Exception {
        when(service.salvar(any())).thenReturn(response());
        when(service.buscarPorId(1L)).thenReturn(response());
        when(service.listar()).thenReturn(List.of(response()));
        when(service.atualizar(any(), any())).thenReturn(response());

        mockMvc.perform(post("/status-ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NOVO\",\"nome\":\"Novo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("COMPRAR_MATERIAL"));
        mockMvc.perform(get("/status-ordens-servico/1")).andExpect(status().isOk());
        mockMvc.perform(get("/status-ordens-servico")).andExpect(status().isOk());
        mockMvc.perform(put("/status-ordens-servico/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Comprar materiais\",\"ativo\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/status-ordens-servico/1"))
                .andExpect(status().isNoContent());
        verify(service).deletar(1L);
    }

    @Test
    void deveValidarCodigoENome() throws Exception {
        mockMvc.perform(post("/status-ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"1 inválido\",\"nome\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.codigo").exists())
                .andExpect(jsonPath("$.erros.nome").exists());
        verifyNoInteractions(service);
    }

    private StatusOrdemServicoResponse response() {
        return StatusOrdemServicoResponse.builder()
                .id(1L).codigo("COMPRAR_MATERIAL")
                .nome("Comprar material").ativo(true).build();
    }
}
