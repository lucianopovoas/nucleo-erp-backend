package br.com.nucleodasreformas.nucleoerp.material.controller;

import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.service.MaterialService;
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
@RequestMapping("/materiais")
@RequiredArgsConstructor
@Tag(name = "Materiais", description = "Operações relacionadas aos Materiais")
public class MaterialController {

    private final MaterialService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar Material")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse salvar(@RequestBody @Valid MaterialRequest request) {

        return service.salvar(request);
    }

    @Operation(summary = "Buscar Material por id")
    @GetMapping("/{id}")
    public MaterialResponse buscarPorId(@PathVariable Long id) {

        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar Materiais")
    @GetMapping
    public List<MaterialResponse> listar() {

        return service.listar();
    }

    @Operation(summary = "Atualizar Material")
    @PutMapping("/{id}")
    public MaterialResponse atualizar(@PathVariable Long id, @RequestBody @Valid MaterialRequest request) {

        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar Material")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {

        service.deletar(id);
    }
}
