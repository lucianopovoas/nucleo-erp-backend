package br.com.nucleodasreformas.nucleoerp.item_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemOrcamentoMapperTest {

    @Test
    void deveCriarEntidadeComValoresResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        Servico servico = Servico.builder().id(5L).nome("Instalação").build();

        ItemOrcamento item = ItemOrcamentoMapper.toEntity(
                orcamento,
                servico,
                "Instalação",
                new BigDecimal("2.5000"),
                new BigDecimal("150.00"),
                new BigDecimal("20.00"),
                new BigDecimal("355.00"));

        assertThat(item.getOrcamento()).isSameAs(orcamento);
        assertThat(item.getServico()).isSameAs(servico);
        assertThat(item.getDescricao()).isEqualTo("Instalação");
        assertThat(item.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(item.getValorUnitario()).isEqualByComparingTo("150.00");
        assertThat(item.getDesconto()).isEqualByComparingTo("20.00");
        assertThat(item.getValorTotal()).isEqualByComparingTo("355.00");
        assertThat(item.getId()).isNull();
    }

    @Test
    void deveAtualizarSomenteCamposEditaveis() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        ItemOrcamento item = ItemOrcamento.builder()
                .id(20L)
                .orcamento(orcamento)
                .servico(Servico.builder().id(1L).nome("Anterior").build())
                .descricao("Anterior")
                .quantidade(BigDecimal.ONE)
                .valorUnitario(BigDecimal.TEN)
                .desconto(BigDecimal.ZERO)
                .valorTotal(BigDecimal.TEN)
                .criadoEm(criadoEm)
                .build();
        Servico novoServico = Servico.builder().id(2L).nome("Novo").build();

        ItemOrcamentoMapper.updateEntity(
                item,
                novoServico,
                "Descrição negociada",
                new BigDecimal("2.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("5.00"),
                new BigDecimal("35.00"));

        assertThat(item.getId()).isEqualTo(20L);
        assertThat(item.getOrcamento()).isSameAs(orcamento);
        assertThat(item.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(item.getServico()).isSameAs(novoServico);
        assertThat(item.getDescricao()).isEqualTo("Descrição negociada");
        assertThat(item.getValorTotal()).isEqualByComparingTo("35.00");
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        ItemOrcamento item = ItemOrcamento.builder()
                .id(20L)
                .servico(Servico.builder().id(5L).nome("Instalação atual").build())
                .descricao("Snapshot negociado")
                .quantidade(new BigDecimal("2.5000"))
                .valorUnitario(new BigDecimal("150.00"))
                .desconto(new BigDecimal("20.00"))
                .valorTotal(new BigDecimal("355.00"))
                .criadoEm(criadoEm)
                .build();

        ItemOrcamentoResponse response = ItemOrcamentoMapper.toResponse(item);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getServico().getId()).isEqualTo(5L);
        assertThat(response.getServico().getNome()).isEqualTo("Instalação atual");
        assertThat(response.getDescricao()).isEqualTo("Snapshot negociado");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(response.getValorUnitario()).isEqualByComparingTo("150.00");
        assertThat(response.getDesconto()).isEqualByComparingTo("20.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("355.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }
}
