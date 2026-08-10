package br.com.nucleodasreformas.nucleoerp.importacao.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional
public class FornecedorImportacaoService {

    private final FornecedorRepository fornecedorRepository;
    private final ExcelReaderService excelReaderService;

    public void importar(MultipartFile arquivo) throws IOException {

        excelReaderService.lerPrimeiraAba(arquivo, sheet -> {

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                Fornecedor fornecedor = Fornecedor.builder()
                        .nome(getTexto(row.getCell(1)))
                        .endereco(getTexto(row.getCell(2)))
                        .celular(getTexto(row.getCell(3)))
                        .contato(getTexto(row.getCell(9)))
                        .build();

                fornecedorRepository.save(fornecedor);
            }
        });
    }

    private String getTexto(Cell cell) {

        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {

            case STRING -> cell.getStringCellValue().trim();

            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());

            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());

            default -> null;
        };

    }

}
