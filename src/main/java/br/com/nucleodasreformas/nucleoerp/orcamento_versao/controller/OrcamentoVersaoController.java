package br.com.nucleodasreformas.nucleoerp.orcamento_versao.controller;

import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/orcamentos/{orcamentoId}/versoes")
@RequiredArgsConstructor
@Tag(name = "Versões de orçamento", description = "Documentos comerciais versionados do orçamento")
public class OrcamentoVersaoController {

    private final OrcamentoVersaoService service;

    @GetMapping
    @Operation(summary = "Listar versões do orçamento")
    public List<OrcamentoVersaoResponse> listar(@PathVariable Long orcamentoId) {
        return service.listar(orcamentoId);
    }

    @GetMapping("/{versaoId}")
    @Operation(summary = "Buscar versão do orçamento")
    public OrcamentoVersaoResponse buscarPorId(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId) {
        return service.buscarPorId(orcamentoId, versaoId);
    }

    @PutMapping("/{versaoId}")
    @Operation(summary = "Atualizar conteúdo da versão em rascunho")
    public OrcamentoVersaoResponse atualizar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @RequestBody @Valid OrcamentoVersaoUpdateRequest request) {
        return service.atualizar(orcamentoId, versaoId, request);
    }

    @PutMapping("/{versaoId}/status")
    @Operation(summary = "Alterar status da versão atual")
    public OrcamentoVersaoResponse alterarStatus(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @RequestBody @Valid OrcamentoVersaoStatusRequest request) {
        return service.alterarStatus(orcamentoId, versaoId, request);
    }

    @PostMapping("/{versaoId}/nova-versao")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar nova versão a partir da versão atual")
    public OrcamentoVersaoResponse criarNovaVersao(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId) {
        return service.criarNovaVersao(orcamentoId, versaoId);
    }
}
