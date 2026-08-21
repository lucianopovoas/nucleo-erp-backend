package br.com.nucleodasreformas.nucleoerp.status_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;

public final class StatusOrcamentoMapper {

    private StatusOrcamentoMapper() {
    }

    public static StatusOrcamento toEntity(StatusOrcamentoRequest request) {
        return StatusOrcamento.builder()
                .codigo(request.getCodigo())
                .nome(request.getNome())
                .build();
    }

    public static StatusOrcamentoResponse toResponse(StatusOrcamento statusOrcamento) {
        return StatusOrcamentoResponse.builder()
                .id(statusOrcamento.getId())
                .codigo(statusOrcamento.getCodigo())
                .nome(statusOrcamento.getNome())
                .ativo(statusOrcamento.getAtivo())
                .criadoEm(statusOrcamento.getCriadoEm())
                .build();
    }

    public static void updateEntity(StatusOrcamento statusOrcamento, StatusOrcamentoUpdateRequest request) {
        statusOrcamento.setNome(request.getNome());
        if (request.getAtivo() != null) {
            statusOrcamento.setAtivo(request.getAtivo());
        }
    }
}
