package br.com.nucleodasreformas.nucleoerp.importacao.service;

import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.EmptyFileException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ImportacaoComportamentoAtualIntegrationTest {

    @Autowired private ClienteImportacaoService clienteService;
    @Autowired private FornecedorImportacaoService fornecedorService;
    @Autowired private MaterialImportacaoService materialService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private MaterialRepository materialRepository;

    @Test
    void deveImportarArquivoValidoDeClientesNoLayoutAtual() throws Exception {
        String nome = "Cliente importado " + UUID.randomUUID();
        var arquivo = planilha("clientes.xlsx", sheet -> {
            sheet.createRow(0).createCell(1).setCellValue("Nome");
            var row = sheet.createRow(1);
            row.createCell(1).setCellValue(nome);
            row.createCell(2).setCellValue("Rua A");
            row.createCell(5).setCellValue(12345678d);
            row.createCell(7).setCellValue("cliente@example.com");
        });

        clienteService.importar(arquivo);
        clienteRepository.flush();

        assertThat(clienteRepository.findAll()).anySatisfy(cliente -> {
            assertThat(cliente.getNome()).isEqualTo(nome);
            assertThat(cliente.getEndereco()).isEqualTo("Rua A");
            assertThat(cliente.getTelefone()).isEqualTo("12345678");
            assertThat(cliente.getEmail()).isEqualTo("cliente@example.com");
        });
    }

    @Test
    void deveImportarFornecedoresEMateriaisNosIndicesAtuais() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        fornecedorService.importar(planilha("fornecedores.xlsx", sheet -> {
            sheet.createRow(0);
            var row = sheet.createRow(1);
            row.createCell(1).setCellValue("Fornecedor " + sufixo);
            row.createCell(2).setCellValue("Rua B");
            row.createCell(3).setCellValue("11999999999");
            row.createCell(9).setCellValue("Contato B");
        }));
        materialService.importar(planilha("materiais.xlsx", sheet -> {
            sheet.createRow(0);
            var row = sheet.createRow(1);
            row.createCell(4).setCellValue("Material " + sufixo);
            row.createCell(6).setCellValue("M2");
        }));
        fornecedorRepository.flush();
        materialRepository.flush();

        assertThat(fornecedorRepository.findAll()).anySatisfy(fornecedor -> {
            assertThat(fornecedor.getNome()).isEqualTo("Fornecedor " + sufixo);
            assertThat(fornecedor.getContato()).isEqualTo("Contato B");
        });
        assertThat(materialRepository.findAll()).anySatisfy(material -> {
            assertThat(material.getNome()).isEqualTo("Material " + sufixo);
            assertThat(material.getUnidade()).isEqualTo("M2");
        });
    }

    @Test
    void deveRejeitarArquivoQueNaoSejaUmaPlanilha() {
        var arquivo = new MockMultipartFile("arquivo", "invalido.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> clienteService.importar(arquivo))
                .isInstanceOf(IOException.class);
    }

    @Test
    void deveRejeitarArquivoSemConteudo() {
        var arquivo = new MockMultipartFile("arquivo", "vazio.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);

        assertThatThrownBy(() -> clienteService.importar(arquivo))
                .isInstanceOf(EmptyFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void deveAceitarPlanilhaComSomenteCabecalhoSemPersistirDados() throws Exception {
        long antes = fornecedorRepository.count();
        var arquivo = planilha("somente-cabecalho.xlsx",
                sheet -> sheet.createRow(0).createCell(1).setCellValue("Nome"));

        fornecedorService.importar(arquivo);

        assertThat(fornecedorRepository.count()).isEqualTo(antes);
    }

    @Test
    void deveIgnorarLinhaSemNomeDeFornecedorOuMaterial() throws Exception {
        long fornecedoresAntes = fornecedorRepository.count();
        long materiaisAntes = materialRepository.count();
        fornecedorService.importar(planilha("fornecedor-sem-nome.xlsx", sheet -> {
            sheet.createRow(0);
            sheet.createRow(1).createCell(2).setCellValue("Sem nome");
        }));
        materialService.importar(planilha("material-sem-nome.xlsx", sheet -> {
            sheet.createRow(0);
            sheet.createRow(1).createCell(6).setCellValue("UN");
        }));

        assertThat(fornecedorRepository.count()).isEqualTo(fornecedoresAntes);
        assertThat(materialRepository.count()).isEqualTo(materiaisAntes);
    }

    @Test
    void devePreservarDuplicidadesPorqueImportacaoAtualNaoAsConsolida() throws Exception {
        String nome = "Duplicado importação " + UUID.randomUUID();
        var arquivo = planilha("duplicados.xlsx", sheet -> {
            sheet.createRow(0);
            sheet.createRow(1).createCell(1).setCellValue(nome);
            sheet.createRow(2).createCell(1).setCellValue(nome);
        });

        clienteService.importar(arquivo);
        clienteRepository.flush();

        assertThat(clienteRepository.findAll().stream()
                .filter(cliente -> nome.equals(cliente.getNome())))
                .hasSize(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveFazerRollbackQuandoClienteInvalidoFalhaNoCommit() throws Exception {
        long antes = clienteRepository.count();
        String nome = "Cliente antes da falha " + UUID.randomUUID();
        var arquivo = planilha("rollback.xlsx", sheet -> {
            sheet.createRow(0);
            sheet.createRow(1).createCell(1).setCellValue(nome);
            sheet.createRow(2).createCell(2).setCellValue("Linha sem nome");
        });

        assertThatThrownBy(() -> clienteService.importar(arquivo))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(clienteRepository.count()).isEqualTo(antes);
        assertThat(clienteRepository.findAll().stream()
                .filter(cliente -> nome.equals(cliente.getNome())))
                .isEmpty();
    }

    private MockMultipartFile planilha(String nome, Consumer<Sheet> conteudo) throws IOException {
        try (var workbook = new XSSFWorkbook();
             var output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dados");
            conteudo.accept(sheet);
            workbook.write(output);
            return new MockMultipartFile("arquivo", nome,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
