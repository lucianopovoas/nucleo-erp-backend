package br.com.nucleodasreformas.nucleoerp.importacao.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ExcelReaderService {

    public Sheet obterPrimeiraAba(MultipartFile arquivo) throws IOException {

        Workbook workbook = WorkbookFactory.create(arquivo.getInputStream());

        return workbook.getSheetAt(0);
    }
}