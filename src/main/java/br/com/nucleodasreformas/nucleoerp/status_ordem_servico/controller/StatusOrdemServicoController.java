package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.service.StatusOrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/status-ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Status de ordem de serviço", description = "Status do fluxo operacional")
public class StatusOrdemServicoController {

    private final StatusOrdemServicoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Salvar status de ordem de serviço")
    public StatusOrdemServicoResponse salvar(
            @RequestBody @Valid StatusOrdemServicoRequest request) {
        return service.salvar(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar status de ordem de serviço por id")
    public StatusOrdemServicoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    @Operation(summary = "Listar status de ordem de serviço ativos")
    public List<StatusOrdemServicoResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar status de ordem de serviço")
    public StatusOrdemServicoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid StatusOrdemServicoUpdateRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar status de ordem de serviço")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
