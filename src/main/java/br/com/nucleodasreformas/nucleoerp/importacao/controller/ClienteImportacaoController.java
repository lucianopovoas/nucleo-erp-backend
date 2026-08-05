package br.com.nucleodasreformas.nucleoerp.importacao.controller;

import br.com.nucleodasreformas.nucleoerp.importacao.service.ClienteImportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/importacoes")
@RequiredArgsConstructor
@Tag(name = "Importacao", description = "Operações relacionadas a importacoes")
public class ClienteImportacaoController {

    private final ClienteImportacaoService clienteImportacaoService;

    @Operation(summary = "Fazer Importacoes")
    @PostMapping(value = "/clientes", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public void importarClientes(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {

        clienteImportacaoService.importar(arquivo);
    }

}