package br.com.nucleodasreformas.nucleoerp.item_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemOrcamentoMapperTest {

    @Test
    void deveMapearEntidadeEAtualizacaoSemRecalcularValores() {
        OrcamentoVersao versao = OrcamentoVersao.builder().id(20L).build();
        Servico servico = Servico.builder().id(5L).nome("Instalação").build();
        ItemOrcamento item = ItemOrcamentoMapper.toEntity(versao, servico, "Snapshot",
                valor("2.0000"), valor("100.00"), valor("10.00"), valor("190.00"));

        assertThat(item.getOrcamentoVersao()).isSameAs(versao);
        assertThat(item.getServico()).isSameAs(servico);
        assertThat(item.getDescricao()).isEqualTo("Snapshot");
        assertThat(item.getValorTotal()).isEqualByComparingTo("190.00");

        ItemOrcamentoMapper.updateEntity(item, servico, "Revisado",
                valor("3.0000"), valor("90.00"), valor("20.00"), valor("250.00"));
        assertThat(item.getDescricao()).isEqualTo("Revisado");
        assertThat(item.getQuantidade()).isEqualByComparingTo("3.0000");
        assertThat(item.getValorTotal()).isEqualByComparingTo("250.00");
    }

    @Test
    void deveConverterSnapshotECatalogoParaResponse() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 23, 10, 0);
        ItemOrcamento item = ItemOrcamento.builder().id(30L)
                .servico(Servico.builder().id(5L).nome("Nome atual").build())
                .descricao("Snapshot negociado").quantidade(valor("2.0000"))
                .valorUnitario(valor("100.00")).desconto(valor("10.00"))
                .valorTotal(valor("190.00")).criadoEm(criadoEm).build();

        var response = ItemOrcamentoMapper.toResponse(item);

        assertThat(response.getServico().getId()).isEqualTo(5L);
        assertThat(response.getServico().getNome()).isEqualTo("Nome atual");
        assertThat(response.getDescricao()).isEqualTo("Snapshot negociado");
        assertThat(response.getValorTotal()).isEqualByComparingTo("190.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    private BigDecimal valor(String valor) { return new BigDecimal(valor); }
}
