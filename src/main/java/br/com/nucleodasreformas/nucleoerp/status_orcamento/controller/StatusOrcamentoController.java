package br.com.nucleodasreformas.nucleoerp.status_orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.service.StatusOrcamentoService;
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
@RequestMapping("/status-orcamentos")
@RequiredArgsConstructor
@Tag(name = "Status de orçamento", description = "Operações relacionadas aos status de orçamento")
public class StatusOrcamentoController {

    private final StatusOrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Status de orçamento cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar status de orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusOrcamentoResponse salvar(@RequestBody @Valid StatusOrcamentoRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar status de orçamento por id")
    @GetMapping("/{id}")
    public StatusOrcamentoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar status de orçamento ativos")
    @GetMapping
    public List<StatusOrcamentoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Atualizar status de orçamento")
    @PutMapping("/{id}")
    public StatusOrcamentoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid StatusOrcamentoUpdateRequest request) {
        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar status de orçamento")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
