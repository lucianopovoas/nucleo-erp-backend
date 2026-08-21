package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
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
@RequestMapping("/orcamentos/{orcamentoId}/despesas")
@RequiredArgsConstructor
@Tag(name = "Despesas do orçamento", description = "Despesas internas previstas do orçamento")
public class DespesaOrcamentoController {

    private final DespesaOrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Despesa incluída no orçamento"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado")
    })
    @Operation(summary = "Incluir despesa prevista no orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DespesaOrcamentoResponse salvar(
            @PathVariable Long orcamentoId,
            @RequestBody @Valid DespesaOrcamentoRequest request) {
        return service.salvar(orcamentoId, request);
    }

    @Operation(summary = "Listar despesas previstas do orçamento")
    @GetMapping
    public List<DespesaOrcamentoResponse> listar(@PathVariable Long orcamentoId) {
        return service.listar(orcamentoId);
    }

    @Operation(summary = "Buscar despesa prevista do orçamento")
    @GetMapping("/{despesaOrcamentoId}")
    public DespesaOrcamentoResponse buscarPorId(
            @PathVariable Long orcamentoId,
            @PathVariable Long despesaOrcamentoId) {
        return service.buscarPorId(orcamentoId, despesaOrcamentoId);
    }

    @Operation(summary = "Atualizar despesa prevista do orçamento")
    @PutMapping("/{despesaOrcamentoId}")
    public DespesaOrcamentoResponse atualizar(
            @PathVariable Long orcamentoId,
            @PathVariable Long despesaOrcamentoId,
            @RequestBody @Valid DespesaOrcamentoUpdateRequest request) {
        return service.atualizar(orcamentoId, despesaOrcamentoId, request);
    }

    @Operation(summary = "Remover despesa prevista do orçamento")
    @DeleteMapping("/{despesaOrcamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable Long orcamentoId,
            @PathVariable Long despesaOrcamentoId) {
        service.deletar(orcamentoId, despesaOrcamentoId);
    }
}
