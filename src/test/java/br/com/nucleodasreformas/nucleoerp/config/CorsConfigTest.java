package br.com.nucleodasreformas.nucleoerp.config;

import br.com.nucleodasreformas.nucleoerp.exception.GlobalExceptionHandler;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.controller.OrdemServicoController;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoFiltroRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.service.OrdemServicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemServicoController.class)
@Import({CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OrdemServicoService service;

    @Test
    void deveAutorizarGetDaOrigemConfiguradaSemCredenciais() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class))).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico").header(ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void deveAutorizarPreflightPutComMetodosEHeadersExplicitos() throws Exception {
        MvcResult result = mockMvc.perform(options("/ordens-servico/30/status")
                        .header(ORIGIN, ALLOWED_ORIGIN)
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, "content-type,accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .andReturn();

        assertThat(result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_METHODS))
                .contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("Content-Type")
                .containsIgnoringCase("Accept");
        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarOrigemNaoConfiguradaAntesDoController() throws Exception {
        mockMvc.perform(get("/ordens-servico")
                        .header(ORIGIN, "http://localhost:9999"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));

        verifyNoInteractions(service);
    }

    @Test
    void deveManterGetSemOriginInalterado() throws Exception {
        when(service.listar(any(OrdemServicoFiltroRequest.class))).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void deveManterProblemDetailParaOrigemPermitida() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(
                new ResourceNotFoundException("Ordem de serviço não encontrada. Id: 99"));

        mockMvc.perform(get("/ordens-servico/99").header(ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isNotFound())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Ordem de serviço não encontrada. Id: 99"));
    }

    @Test
    void deveRejeitarWildcardNaConfiguracao() {
        assertThatThrownBy(() -> new CorsConfig("http://localhost:5173, https://*.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.cors.allowed-origins")
                .hasMessageContaining("wildcard");
    }
}
