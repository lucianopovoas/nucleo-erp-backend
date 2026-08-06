package br.com.nucleodasreformas.nucleoerp.fornecedor.controller;

import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Operações relacionadas aos fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    @Operation(summary = "Cadastrar Fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fornecedor cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse salvar(@RequestBody @Valid FornecedorRequest request){

        return service.salvar(request);
    }

    @Operation(summary = "Buscar Fornecedor por id")
    @GetMapping("/{id}")
    public FornecedorResponse buscarPorId(@PathVariable Long id) {

        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar Fornecedores")
    @GetMapping
    public List<FornecedorResponse> listar(){

        return service.listar();
    }

    @Operation(summary = "Atualizar Fornecedor")
    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @RequestBody @Valid FornecedorRequest request){

        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar Fornecedor")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id){

        service.deletar(id);
    }
}
