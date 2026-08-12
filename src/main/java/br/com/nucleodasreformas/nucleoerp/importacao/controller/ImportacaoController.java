package br.com.nucleodasreformas.nucleoerp.importacao.controller;

import br.com.nucleodasreformas.nucleoerp.importacao.service.ClienteImportacaoService;
import br.com.nucleodasreformas.nucleoerp.importacao.service.FornecedorImportacaoService;
import br.com.nucleodasreformas.nucleoerp.importacao.service.MaterialImportacaoService;
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
public class ImportacaoController {

    private final ClienteImportacaoService clienteImportacaoService;
    private final FornecedorImportacaoService fornecedorImportacaoService;
    private final MaterialImportacaoService materialImportacaoService;

    @Operation(summary = "Fazer Importacao Clientes")
    @PostMapping(value = "/clientes", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public void importarClientes(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {

        clienteImportacaoService.importar(arquivo);
    }

    @Operation(summary = "Fazer Importacao Fornecedores")
    @PostMapping(value = "/fornecedores", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public void importarFornecedores(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {

        fornecedorImportacaoService.importar(arquivo);
    }

    @Operation(summary = "Fazer Importacao Materiais")
    @PostMapping(value = "/materiais", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public void importarMateriais(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {

        materialImportacaoService.importar(arquivo);
    }

}