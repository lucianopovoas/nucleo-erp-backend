package br.com.nucleodasreformas.nucleoerp.materialfornecedor.controller;

import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.service.MaterialFornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/materiais-fornecedores")
@RequiredArgsConstructor
@Tag(name = "Materiais e fornecedores", description = "Operações relacionadas às ofertas de materiais por fornecedores")
public class MaterialFornecedorController {

    private final MaterialFornecedorService service;

    @Operation(summary = "Cadastrar ou reativar vínculo entre material e fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vínculo cadastrado ou reativado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou vínculo ativo duplicado"),
            @ApiResponse(responseCode = "404", description = "Material ou fornecedor não encontrado")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialFornecedorResponse salvar(@RequestBody @Valid MaterialFornecedorRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar vínculo por id")
    @GetMapping("/{id}")
    public MaterialFornecedorResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar vínculos ativos")
    @GetMapping
    public List<MaterialFornecedorResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Atualizar vínculo ativo")
    @PutMapping("/{id}")
    public MaterialFornecedorResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid MaterialFornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @Operation(summary = "Inativar vínculo")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
