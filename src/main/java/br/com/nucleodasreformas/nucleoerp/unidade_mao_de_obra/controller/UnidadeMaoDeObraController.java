package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.controller;

import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraRequest;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.service.UnidadeMaoDeObraService;
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
@RequestMapping("/unidades-mao-de-obra")
@RequiredArgsConstructor
@Tag(name = "Unidades de mão de obra", description = "Operações relacionadas às unidades de mão de obra")
public class UnidadeMaoDeObraController {

    private final UnidadeMaoDeObraService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Unidade de mão de obra cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar unidade de mão de obra")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeMaoDeObraResponse salvar(
            @RequestBody @Valid UnidadeMaoDeObraRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar unidade de mão de obra por id")
    @GetMapping("/{id}")
    public UnidadeMaoDeObraResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar unidades de mão de obra ativas")
    @GetMapping
    public List<UnidadeMaoDeObraResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Atualizar unidade de mão de obra")
    @PutMapping("/{id}")
    public UnidadeMaoDeObraResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UnidadeMaoDeObraRequest request) {
        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar unidade de mão de obra")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
