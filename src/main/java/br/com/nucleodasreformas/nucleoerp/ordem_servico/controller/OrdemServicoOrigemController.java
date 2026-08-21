package br.com.nucleodasreformas.nucleoerp.ordem_servico.controller;

import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.service.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de serviço", description = "Processo operacional originado do orçamento")
public class OrdemServicoOrigemController {

    private final OrdemServicoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar ordem de serviço a partir da versão aprovada")
    public OrdemServicoResponse salvar(
            @PathVariable Long orcamentoId,
            @PathVariable Long versaoId) {
        return service.salvar(orcamentoId, versaoId);
    }
}
