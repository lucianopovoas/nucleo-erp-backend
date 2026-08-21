package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service.MaoDeObraOrcamentoService;
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
@RequestMapping("/orcamentos/{orcamentoId}/versoes/{versaoId}/mao-de-obra")
@RequiredArgsConstructor
@Tag(name = "Mão de obra do orçamento", description = "Custos previstos de mão de obra do orçamento")
public class MaoDeObraOrcamentoController {

    private final MaoDeObraOrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mão de obra incluída no orçamento"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Orçamento ou unidade não encontrada")
    })
    @Operation(summary = "Incluir mão de obra prevista no orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaoDeObraOrcamentoResponse salvar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @RequestBody @Valid MaoDeObraOrcamentoRequest request) {
        return service.salvar(orcamentoId, versaoId, request);
    }

    @Operation(summary = "Listar mão de obra prevista do orçamento")
    @GetMapping
    public List<MaoDeObraOrcamentoResponse> listar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId) {
        return service.listar(orcamentoId, versaoId);
    }

    @Operation(summary = "Buscar mão de obra prevista do orçamento")
    @GetMapping("/{maoDeObraOrcamentoId}")
    public MaoDeObraOrcamentoResponse buscarPorId(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long maoDeObraOrcamentoId) {
        return service.buscarPorId(orcamentoId, versaoId, maoDeObraOrcamentoId);
    }

    @Operation(summary = "Atualizar mão de obra prevista do orçamento")
    @PutMapping("/{maoDeObraOrcamentoId}")
    public MaoDeObraOrcamentoResponse atualizar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long maoDeObraOrcamentoId,
            @RequestBody @Valid MaoDeObraOrcamentoUpdateRequest request) {
        return service.atualizar(orcamentoId, versaoId, maoDeObraOrcamentoId, request);
    }

    @Operation(summary = "Remover mão de obra prevista do orçamento")
    @DeleteMapping("/{maoDeObraOrcamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long maoDeObraOrcamentoId) {
        service.deletar(orcamentoId, versaoId, maoDeObraOrcamentoId);
    }
}
