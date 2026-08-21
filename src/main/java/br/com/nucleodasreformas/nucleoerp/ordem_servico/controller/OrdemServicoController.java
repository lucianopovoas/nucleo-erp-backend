package br.com.nucleodasreformas.nucleoerp.ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.service.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de serviço", description = "Processo operacional originado do orçamento")
public class OrdemServicoController {

    private final OrdemServicoService service;

    @GetMapping
    @Operation(summary = "Listar ordens de serviço")
    public List<OrdemServicoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{ordemServicoId}")
    @Operation(summary = "Buscar ordem de serviço por id")
    public OrdemServicoResponse buscarPorId(@PathVariable Long ordemServicoId) {
        return service.buscarPorId(ordemServicoId);
    }

    @PutMapping("/{ordemServicoId}")
    @Operation(summary = "Atualizar observação da ordem de serviço")
    public OrdemServicoResponse atualizar(
            @PathVariable Long ordemServicoId,
            @RequestBody @Valid OrdemServicoUpdateRequest request) {
        return service.atualizar(ordemServicoId, request);
    }

    @PutMapping("/{ordemServicoId}/status")
    @Operation(summary = "Alterar status da ordem de serviço")
    public OrdemServicoResponse alterarStatus(
            @PathVariable Long ordemServicoId,
            @RequestBody @Valid OrdemServicoStatusRequest request) {
        return service.alterarStatus(ordemServicoId, request);
    }
}
