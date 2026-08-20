package br.com.nucleodasreformas.nucleoerp.categoria_servico.controller;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.service.CategoriaServicoService;
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
@RequestMapping("/categorias-servico")
@RequiredArgsConstructor
@Tag(name = "Categorias de serviço", description = "Operações relacionadas às categorias de serviço")
public class CategoriaServicoController {

    private final CategoriaServicoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria de serviço cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar categoria de serviço")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaServicoResponse salvar(@RequestBody @Valid CategoriaServicoRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar categoria de serviço por id")
    @GetMapping("/{id}")
    public CategoriaServicoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar categorias de serviço ativas")
    @GetMapping
    public List<CategoriaServicoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Atualizar categoria de serviço")
    @PutMapping("/{id}")
    public CategoriaServicoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CategoriaServicoRequest request) {
        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar categoria de serviço")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
