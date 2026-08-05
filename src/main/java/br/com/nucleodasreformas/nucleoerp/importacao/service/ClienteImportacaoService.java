package br.com.nucleodasreformas.nucleoerp.importacao.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteImportacaoService {

    private final ClienteRepository clienteRepository;
    private final ExcelReaderService excelReaderService;

    public void importar(MultipartFile arquivo) throws IOException {

        excelReaderService.lerPrimeiraAba(arquivo, sheet -> {

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                Cliente cliente = Cliente.builder()
                        .nome(getTexto(row.getCell(1)))
                        .endereco(getTexto(row.getCell(2)))
                        .cnpj(getTexto(row.getCell(3)))
                        .cpf(getTexto(row.getCell(4)))
                        .telefone(getTexto(row.getCell(5)))
                        .celular(getTexto(row.getCell(6)))
                        .email(getTexto(row.getCell(7)))
                        .contato(getTexto(row.getCell(8)))
                        .build();

                clienteRepository.save(cliente);
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