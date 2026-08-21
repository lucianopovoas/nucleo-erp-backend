package br.com.nucleodasreformas.nucleoerp.item_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
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
@RequestMapping("/orcamentos/{orcamentoId}/versoes/{versaoId}/itens")
@RequiredArgsConstructor
@Tag(name = "Itens de orçamento", description = "Serviços negociados em um orçamento")
public class ItemOrcamentoController {

    private final ItemOrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item incluído no orçamento"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Orçamento ou serviço não encontrado")
    })
    @Operation(summary = "Incluir item no orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemOrcamentoResponse salvar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @RequestBody @Valid ItemOrcamentoRequest request) {
        return service.salvar(orcamentoId, versaoId, request);
    }

    @Operation(summary = "Listar itens do orçamento")
    @GetMapping
    public List<ItemOrcamentoResponse> listar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId) {
        return service.listar(orcamentoId, versaoId);
    }

    @Operation(summary = "Buscar item do orçamento")
    @GetMapping("/{itemId}")
    public ItemOrcamentoResponse buscarPorId(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long itemId) {
        return service.buscarPorId(orcamentoId, versaoId, itemId);
    }

    @Operation(summary = "Atualizar item do orçamento")
    @PutMapping("/{itemId}")
    public ItemOrcamentoResponse atualizar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemOrcamentoUpdateRequest request) {
        return service.atualizar(orcamentoId, versaoId, itemId, request);
    }

    @Operation(summary = "Remover item do orçamento")
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId,
            @PathVariable Long itemId) {
        service.deletar(orcamentoId, versaoId, itemId);
    }
}
