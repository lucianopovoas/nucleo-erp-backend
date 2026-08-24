package br.com.nucleodasreformas.nucleoerp.exception;

import br.com.nucleodasreformas.nucleoerp.importacao.controller.ImportacaoController;
import br.com.nucleodasreformas.nucleoerp.importacao.service.ClienteImportacaoService;
import br.com.nucleodasreformas.nucleoerp.importacao.service.FornecedorImportacaoService;
import br.com.nucleodasreformas.nucleoerp.importacao.service.MaterialImportacaoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.controller.OrcamentoVersaoController;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoService;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.controller.OrdemServicoController;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.service.OrdemServicoService;
import org.apache.poi.EmptyFileException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        OrcamentoVersaoController.class,
        OrdemServicoController.class,
        ImportacaoController.class
})
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerHttpTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OrcamentoVersaoService orcamentoVersaoService;
    @MockitoBean private OrdemServicoService ordemServicoService;
    @MockitoBean private ClienteImportacaoService clienteImportacaoService;
    @MockitoBean private FornecedorImportacaoService fornecedorImportacaoService;
    @MockitoBean private MaterialImportacaoService materialImportacaoService;

    @Test
    void devePadronizarJsonMalformado() throws Exception {
        mockMvc.perform(put("/orcamentos/10/versoes/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrcamentoId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("JSON inválido"))
                .andExpect(jsonPath("$.detail")
                        .value("O corpo da requisição não contém um JSON válido."));

        verifyNoInteractions(orcamentoVersaoService);
    }

    @Test
    void devePadronizarPathVariableInvalida() throws Exception {
        mockMvc.perform(get("/ordens-servico/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.erros.ordemServicoId").exists());

        verifyNoInteractions(ordemServicoService);
    }

    @Test
    void devePadronizarQueryParameterInvalido() throws Exception {
        mockMvc.perform(get("/ordens-servico").param("criadoDe", "23-08-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.criadoDe").exists());

        verifyNoInteractions(ordemServicoService);
    }

    @Test
    void devePadronizarArquivoMultipartAusente() throws Exception {
        mockMvc.perform(multipart("/importacoes/clientes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros.arquivo").exists());

        verifyNoInteractions(clienteImportacaoService);
    }

    @Test
    void devePadronizarArquivoVazio() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "vazio.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
        doThrow(new EmptyFileException()).when(clienteImportacaoService).importar(any());

        mockMvc.perform(multipart("/importacoes/clientes").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro ao importar arquivo"))
                .andExpect(jsonPath("$.detail").value("O arquivo enviado está vazio."));
    }

    @Test
    void devePadronizarArquivoAcimaDoLimite() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "grande.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});
        doThrow(new MaxUploadSizeExceededException(1)).when(clienteImportacaoService).importar(any());

        mockMvc.perform(multipart("/importacoes/clientes").file(arquivo))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Arquivo muito grande"))
                .andExpect(jsonPath("$.detail")
                        .value("O arquivo excede o limite aceito pela aplicação."));
    }

    @Test
    void devePadronizarMultipartInvalido() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "invalido.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});
        doThrow(new MultipartException("multipart inválido"))
                .when(clienteImportacaoService).importar(any());

        mockMvc.perform(multipart("/importacoes/clientes").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Multipart inválido"))
                .andExpect(jsonPath("$.detail")
                        .value("A requisição multipart não pôde ser processada."));
    }

    @Test
    void devePadronizarContentTypeNaoSuportado() throws Exception {
        mockMvc.perform(put("/orcamentos/10/versoes/20/status")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("status=2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Tipo de conteúdo não suportado"));

        verifyNoInteractions(orcamentoVersaoService);
    }

    @Test
    void devePadronizarMetodoHttpNaoPermitido() throws Exception {
        mockMvc.perform(delete("/ordens-servico/30"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método HTTP não permitido"));

        verifyNoInteractions(ordemServicoService);
    }
}
