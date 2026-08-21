package br.com.nucleodasreformas.nucleoerp.material_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
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
@RequestMapping("/orcamentos/{orcamentoId}/materiais")
@RequiredArgsConstructor
@Tag(name = "Materiais do orçamento", description = "Custos previstos de materiais do orçamento")
public class MaterialOrcamentoController {

    private final MaterialOrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material incluído no orçamento"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Orçamento ou material não encontrado")
    })
    @Operation(summary = "Incluir material previsto no orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialOrcamentoResponse salvar(
            @PathVariable Long orcamentoId,
            @RequestBody @Valid MaterialOrcamentoRequest request) {
        return service.salvar(orcamentoId, request);
    }

    @Operation(summary = "Listar materiais previstos do orçamento")
    @GetMapping
    public List<MaterialOrcamentoResponse> listar(@PathVariable Long orcamentoId) {
        return service.listar(orcamentoId);
    }

    @Operation(summary = "Buscar material previsto do orçamento")
    @GetMapping("/{materialOrcamentoId}")
    public MaterialOrcamentoResponse buscarPorId(
            @PathVariable Long orcamentoId,
            @PathVariable Long materialOrcamentoId) {
        return service.buscarPorId(orcamentoId, materialOrcamentoId);
    }

    @Operation(summary = "Atualizar material previsto do orçamento")
    @PutMapping("/{materialOrcamentoId}")
    public MaterialOrcamentoResponse atualizar(
            @PathVariable Long orcamentoId,
            @PathVariable Long materialOrcamentoId,
            @RequestBody @Valid MaterialOrcamentoUpdateRequest request) {
        return service.atualizar(orcamentoId, materialOrcamentoId, request);
    }

    @Operation(summary = "Remover material previsto do orçamento")
    @DeleteMapping("/{materialOrcamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable Long orcamentoId,
            @PathVariable Long materialOrcamentoId) {
        service.deletar(orcamentoId, materialOrcamentoId);
    }
}
