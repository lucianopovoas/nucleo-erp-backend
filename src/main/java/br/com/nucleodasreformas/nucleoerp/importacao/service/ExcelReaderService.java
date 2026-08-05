package br.com.nucleodasreformas.nucleoerp.importacao.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.function.Consumer;

@Service
public class ExcelReaderService {

    public void lerPrimeiraAba(MultipartFile arquivo,
                               Consumer<Sheet> processador) throws IOException {

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            processador.accept(sheet);

        }
    }

}