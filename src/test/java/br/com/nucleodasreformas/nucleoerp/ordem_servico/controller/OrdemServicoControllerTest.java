package br.com.nucleodasreformas.nucleoerp.ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrcamentoOrigemResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrcamentoVersaoOrigemResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoFiltroRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoOrigemResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.StatusOrdemServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.service.OrdemServicoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest({OrdemServicoController.class, OrdemServicoOrigemController.class})
@Import(GlobalExceptionHandler.class)
class OrdemServicoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OrdemServicoService service;

    @Test
    void deveCriarSomentePelaRotaContextual() throws Exception {
        when(service.salvar(10L, 20L)).thenReturn(response());

        mockMvc.perform(post("/orcamentos/10/versoes/20/ordem-servico"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(45))
                .andExpect(jsonPath("$.status.codigo").value("COMPRAR_MATERIAL"))
                .andExpect(jsonPath("$.origem.orcamento.id").value(10))
                .andExpect(jsonPath("$.origem.versao.id").value(20))
                .andExpect(jsonPath("$.origem.cliente.nome").value("Cliente"));

        mockMvc.perform(post("/ordens-servico"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void deveListarBuscarAtualizarEAlterarStatus() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class))).thenReturn(List.of(response()));
        when(service.buscarPorId(30L)).thenReturn(response());
        when(service.atualizar(any(), any())).thenReturn(response());
        when(service.alterarStatus(any(), any())).thenReturn(response());

        mockMvc.perform(get("/ordens-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
        mockMvc.perform(get("/ordens-servico/30"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/ordens-servico/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Nova\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/ordens-servico/30/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrdemServicoId\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveVincularFiltrosOpcionaisDaListagem() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class))).thenReturn(List.of(response()));

        mockMvc.perform(get("/ordens-servico")
                        .param("numero", "45")
                        .param("status", " INSTALAR ")
                        .param("clienteId", "10")
                        .param("orcamentoId", "25")
                        .param("criadoDe", "2026-08-01")
                        .param("criadoAte", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));

        ArgumentCaptor<OrdemServicoFiltroRequest> captor =
                ArgumentCaptor.forClass(OrdemServicoFiltroRequest.class);
        verify(service).listar(captor.capture());
        OrdemServicoFiltroRequest filtros = captor.getValue();
        assertThat(filtros.getNumero()).isEqualTo(45L);
        assertThat(filtros.getStatus()).isEqualTo(" INSTALAR ");
        assertThat(filtros.getClienteId()).isEqualTo(10L);
        assertThat(filtros.getOrcamentoId()).isEqualTo(25L);
        assertThat(filtros.getCriadoDe())
                .isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(filtros.getCriadoAte())
                .isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @ParameterizedTest
    @CsvSource({
            "numero, abc, numero",
            "clienteId, abc, clienteId",
            "orcamentoId, abc, orcamentoId",
            "criadoDe, 22-08-2026, criadoDe",
            "criadoAte, data-invalida, criadoAte"
    })
    void deveManterProblemDetailParaFalhaDeBinding(
            String parametro, String valor, String campo) throws Exception {
        mockMvc.perform(get("/ordens-servico").param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail")
                        .value("Um ou mais campos estão inválidos."))
                .andExpect(jsonPath("$.erros." + campo).exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void deveRejeitarStatusVazioOuSomenteComEspacos(String statusInformado) throws Exception {
        mockMvc.perform(get("/ordens-servico").param("status", statusInformado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.status").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarStatusComMaisDeCinquentaCaracteres() throws Exception {
        mockMvc.perform(get("/ordens-servico").param("status", "A".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.status").exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarColecaoVaziaQuandoNaoHouverCorrespondencia() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class))).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico").param("numero", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "numero, 0, numero",
            "numero, -1, numero",
            "clienteId, 0, clienteId",
            "clienteId, -1, clienteId",
            "orcamentoId, 0, orcamentoId",
            "orcamentoId, -1, orcamentoId"
    })
    void deveRejeitarFiltrosNumericosNaoPositivos(
            String parametro, String valor, String campo) throws Exception {
        mockMvc.perform(get("/ordens-servico").param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros." + campo).exists());

        verifyNoInteractions(service);
    }

    @Test
    void deveManterProblemDetailParaIntervaloInvertido() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class)))
                .thenThrow(new BusinessException(
                        "A data inicial de criação não pode ser posterior à data final."));

        mockMvc.perform(get("/ordens-servico")
                        .param("criadoDe", "2026-08-31")
                        .param("criadoAte", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"))
                .andExpect(jsonPath("$.detail")
                        .value("A data inicial de criação não pode ser posterior à data final."));
    }

    @Test
    void deveValidarStatusERejeitarDelete() throws Exception {
        mockMvc.perform(put("/ordens-servico/30/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.statusOrdemServicoId").exists());
        verifyNoInteractions(service);

        mockMvc.perform(delete("/ordens-servico/30"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void deveManterContratoProblemDetail() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(
                new ResourceNotFoundException("Ordem de serviço não encontrada. Id: 99"));
        when(service.salvar(10L, 20L)).thenThrow(
                new BusinessException("A ordem de serviço só pode ser criada a partir de uma versão APROVADA."));

        mockMvc.perform(get("/ordens-servico/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
        mockMvc.perform(post("/orcamentos/10/versoes/20/ordem-servico"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de negócio"));
    }

    private OrdemServicoResponse response() {
        return OrdemServicoResponse.builder()
                .id(30L)
                .numero(45L)
                .status(StatusOrdemServicoResumoResponse.builder()
                        .id(1L).codigo("COMPRAR_MATERIAL").nome("Comprar material").build())
                .criadoEm(LocalDateTime.of(2026, 8, 21, 10, 0))
                .origem(OrdemServicoOrigemResponse.builder()
                        .orcamento(OrcamentoOrigemResumoResponse.builder()
                                .id(10L).numero(125L).build())
                        .versao(OrcamentoVersaoOrigemResumoResponse.builder()
                                .id(20L).numeroVersao(3).build())
                        .cliente(ClienteResumoResponse.builder()
                                .id(40L).nome("Cliente").build())
                        .build())
                .build();
    }
}
