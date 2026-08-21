package br.com.nucleodasreformas.nucleoerp.orcamento_versao.mapper;

import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.TotaisOrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;

public final class OrcamentoVersaoMapper {

    private OrcamentoVersaoMapper() {
    }

    public static OrcamentoVersao toEntity(
            Orcamento orcamento,
            int numeroVersao,
            StatusOrcamento status,
            String observacao) {
        return OrcamentoVersao.builder()
                .orcamento(orcamento)
                .numeroVersao(numeroVersao)
                .statusOrcamento(status)
                .observacao(observacao)
                .build();
    }

    public static OrcamentoVersaoResponse toResponse(
            OrcamentoVersao versao,
            TotaisOrcamentoVersao totais) {
        StatusOrcamento status = versao.getStatusOrcamento();
        return OrcamentoVersaoResponse.builder()
                .id(versao.getId())
                .numeroVersao(versao.getNumeroVersao())
                .status(StatusOrcamentoResumoResponse.builder()
                        .id(status.getId())
                        .codigo(status.getCodigo())
                        .nome(status.getNome())
                        .build())
                .observacao(versao.getObservacao())
                .totalComercial(totais.totalComercial())
                .custoTotalMateriais(totais.custoTotalMateriais())
                .custoTotalMaoDeObra(totais.custoTotalMaoDeObra())
                .custoTotalDespesas(totais.custoTotalDespesas())
                .margemPrevista(totais.margemPrevista())
                .percentualMargem(totais.percentualMargem())
                .criadoEm(versao.getCriadoEm())
                .build();
    }
}
