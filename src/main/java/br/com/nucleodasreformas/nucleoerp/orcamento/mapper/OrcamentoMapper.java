package br.com.nucleodasreformas.nucleoerp.orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;

import java.math.BigDecimal;

public final class OrcamentoMapper {

    private OrcamentoMapper() {
    }

    public static Orcamento toEntity(
            OrcamentoRequest request,
            Cliente cliente,
            StatusOrcamento statusOrcamento) {

        return Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(statusOrcamento)
                .observacao(request.getObservacao())
                .build();
    }

    public static void updateEntity(
            Orcamento orcamento,
            OrcamentoUpdateRequest request,
            Cliente cliente,
            StatusOrcamento statusOrcamento) {

        orcamento.setCliente(cliente);
        orcamento.setStatusOrcamento(statusOrcamento);
        if (request.isObservacaoInformada()) {
            orcamento.setObservacao(request.getObservacao());
        }
    }

    public static OrcamentoResponse toResponse(
            Orcamento orcamento,
            BigDecimal totalComercial,
            BigDecimal custoTotalMateriais,
            BigDecimal custoTotalMaoDeObra,
            BigDecimal custoTotalDespesas,
            BigDecimal margemPrevista,
            BigDecimal percentualMargem) {
        Cliente cliente = orcamento.getCliente();
        StatusOrcamento statusOrcamento = orcamento.getStatusOrcamento();

        return OrcamentoResponse.builder()
                .id(orcamento.getId())
                .numero(orcamento.getNumero())
                .cliente(ClienteResumoResponse.builder()
                        .id(cliente.getId())
                        .nome(cliente.getNome())
                        .build())
                .status(StatusOrcamentoResumoResponse.builder()
                        .id(statusOrcamento.getId())
                        .nome(statusOrcamento.getNome())
                        .build())
                .observacao(orcamento.getObservacao())
                .totalComercial(totalComercial)
                .custoTotalMateriais(custoTotalMateriais)
                .custoTotalMaoDeObra(custoTotalMaoDeObra)
                .custoTotalDespesas(custoTotalDespesas)
                .margemPrevista(margemPrevista)
                .percentualMargem(percentualMargem)
                .criadoEm(orcamento.getCriadoEm())
                .build();
    }
}
