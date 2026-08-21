package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;

import java.math.BigDecimal;

public final class DespesaOrcamentoMapper {

    private DespesaOrcamentoMapper() {
    }

    public static DespesaOrcamento toEntity(
            OrcamentoVersao orcamentoVersao,
            String descricao,
            BigDecimal valor) {
        return DespesaOrcamento.builder()
                .orcamentoVersao(orcamentoVersao)
                .descricao(descricao)
                .valor(valor)
                .build();
    }

    public static void updateEntity(
            DespesaOrcamento despesaOrcamento,
            String descricao,
            BigDecimal valor) {
        despesaOrcamento.setDescricao(descricao);
        despesaOrcamento.setValor(valor);
    }

    public static DespesaOrcamentoResponse toResponse(DespesaOrcamento despesaOrcamento) {
        return DespesaOrcamentoResponse.builder()
                .id(despesaOrcamento.getId())
                .descricao(despesaOrcamento.getDescricao())
                .valor(despesaOrcamento.getValor())
                .criadoEm(despesaOrcamento.getCriadoEm())
                .build();
    }
}
