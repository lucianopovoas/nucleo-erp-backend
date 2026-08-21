package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;

public final class StatusOrdemServicoMapper {

    private StatusOrdemServicoMapper() {
    }

    public static StatusOrdemServico toEntity(StatusOrdemServicoRequest request) {
        return StatusOrdemServico.builder()
                .codigo(request.getCodigo())
                .nome(request.getNome())
                .build();
    }

    public static StatusOrdemServicoResponse toResponse(StatusOrdemServico status) {
        return StatusOrdemServicoResponse.builder()
                .id(status.getId())
                .codigo(status.getCodigo())
                .nome(status.getNome())
                .ativo(status.getAtivo())
                .criadoEm(status.getCriadoEm())
                .build();
    }

    public static void updateEntity(
            StatusOrdemServico status, StatusOrdemServicoUpdateRequest request) {
        status.setNome(request.getNome());
        if (request.getAtivo() != null) {
            status.setAtivo(request.getAtivo());
        }
    }
}
