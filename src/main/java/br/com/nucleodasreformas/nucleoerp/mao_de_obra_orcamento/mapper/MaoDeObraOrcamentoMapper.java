package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.UnidadeMaoDeObraResumoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;

import java.math.BigDecimal;

public final class MaoDeObraOrcamentoMapper {

    private MaoDeObraOrcamentoMapper() {
    }

    public static MaoDeObraOrcamento toEntity(
            OrcamentoVersao orcamentoVersao,
            UnidadeMaoDeObra unidadeMaoDeObra,
            String descricao,
            String unidade,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            BigDecimal custoTotal) {

        return MaoDeObraOrcamento.builder()
                .orcamentoVersao(orcamentoVersao)
                .unidadeMaoDeObra(unidadeMaoDeObra)
                .descricao(descricao)
                .unidade(unidade)
                .quantidade(quantidade)
                .custoUnitario(custoUnitario)
                .custoTotal(custoTotal)
                .build();
    }

    public static void updateEntity(
            MaoDeObraOrcamento maoDeObraOrcamento,
            UnidadeMaoDeObra unidadeMaoDeObra,
            String descricao,
            String unidade,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            BigDecimal custoTotal) {

        maoDeObraOrcamento.setUnidadeMaoDeObra(unidadeMaoDeObra);
        maoDeObraOrcamento.setDescricao(descricao);
        maoDeObraOrcamento.setUnidade(unidade);
        maoDeObraOrcamento.setQuantidade(quantidade);
        maoDeObraOrcamento.setCustoUnitario(custoUnitario);
        maoDeObraOrcamento.setCustoTotal(custoTotal);
    }

    public static MaoDeObraOrcamentoResponse toResponse(
            MaoDeObraOrcamento maoDeObraOrcamento) {
        UnidadeMaoDeObra unidadeMaoDeObra = maoDeObraOrcamento.getUnidadeMaoDeObra();

        return MaoDeObraOrcamentoResponse.builder()
                .id(maoDeObraOrcamento.getId())
                .unidadeMaoDeObra(UnidadeMaoDeObraResumoResponse.builder()
                        .id(unidadeMaoDeObra.getId())
                        .nome(unidadeMaoDeObra.getNome())
                        .build())
                .descricao(maoDeObraOrcamento.getDescricao())
                .unidade(maoDeObraOrcamento.getUnidade())
                .quantidade(maoDeObraOrcamento.getQuantidade())
                .custoUnitario(maoDeObraOrcamento.getCustoUnitario())
                .custoTotal(maoDeObraOrcamento.getCustoTotal())
                .criadoEm(maoDeObraOrcamento.getCriadoEm())
                .build();
    }
}
