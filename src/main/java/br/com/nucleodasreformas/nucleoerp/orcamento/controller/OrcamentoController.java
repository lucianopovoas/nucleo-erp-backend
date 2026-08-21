package br.com.nucleodasreformas.nucleoerp.orcamento.controller;

import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
@Tag(name = "Orçamentos", description = "Operações relacionadas à negociação raiz do orçamento")
public class OrcamentoController {

    private final OrcamentoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orçamento cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar orçamento")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse salvar(@RequestBody @Valid OrcamentoRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar orçamento por id")
    @GetMapping("/{id}")
    public OrcamentoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar orçamentos")
    @GetMapping
    public List<OrcamentoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Corrigir cliente do orçamento inicial")
    @PutMapping("/{id}")
    public OrcamentoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid OrcamentoUpdateRequest request) {
        return service.atualizar(id, request);
    }
}
