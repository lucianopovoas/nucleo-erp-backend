package br.com.nucleodasreformas.nucleoerp.importacao.service;

import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
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
public class MaterialImportacaoService {

    private final MaterialRepository materialRepository;
    private final ExcelReaderService excelReaderService;

    public void importar(MultipartFile arquivo) throws IOException {

        excelReaderService.lerPrimeiraAba(arquivo, sheet -> {

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String nome = getTexto(row.getCell(4));

                if (nome == null || nome.isBlank()) {
                    continue;
                }

                Material material = Material.builder()
                        .nome(nome)
                        .descricao(null)
                        .unidade(getTexto(row.getCell(6)))
                        .build();

                materialRepository.save(material);
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
