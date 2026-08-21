package br.com.nucleodasreformas.nucleoerp.item_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;

import java.math.BigDecimal;

public final class ItemOrcamentoMapper {

    private ItemOrcamentoMapper() {
    }

    public static ItemOrcamento toEntity(
            Orcamento orcamento,
            Servico servico,
            String descricao,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal desconto,
            BigDecimal valorTotal) {

        return ItemOrcamento.builder()
                .orcamento(orcamento)
                .servico(servico)
                .descricao(descricao)
                .quantidade(quantidade)
                .valorUnitario(valorUnitario)
                .desconto(desconto)
                .valorTotal(valorTotal)
                .build();
    }

    public static void updateEntity(
            ItemOrcamento item,
            Servico servico,
            String descricao,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal desconto,
            BigDecimal valorTotal) {

        item.setServico(servico);
        item.setDescricao(descricao);
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        item.setDesconto(desconto);
        item.setValorTotal(valorTotal);
    }

    public static ItemOrcamentoResponse toResponse(ItemOrcamento item) {
        Servico servico = item.getServico();

        return ItemOrcamentoResponse.builder()
                .id(item.getId())
                .servico(ServicoResumoResponse.builder()
                        .id(servico.getId())
                        .nome(servico.getNome())
                        .build())
                .descricao(item.getDescricao())
                .quantidade(item.getQuantidade())
                .valorUnitario(item.getValorUnitario())
                .desconto(item.getDesconto())
                .valorTotal(item.getValorTotal())
                .criadoEm(item.getCriadoEm())
                .build();
    }
}
