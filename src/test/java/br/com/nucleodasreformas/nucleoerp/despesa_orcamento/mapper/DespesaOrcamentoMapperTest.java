package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DespesaOrcamentoMapperTest {

    @Test
    void deveMapearEntidadeEAtualizacaoSemAlterarContexto() {
        OrcamentoVersao versao = OrcamentoVersao.builder().id(20L).build();
        DespesaOrcamento despesa = DespesaOrcamentoMapper.toEntity(
                versao, "Frete", new BigDecimal("180.00"));

        assertThat(despesa.getOrcamentoVersao()).isSameAs(versao);
        assertThat(despesa.getDescricao()).isEqualTo("Frete");
        assertThat(despesa.getValor()).isEqualByComparingTo("180.00");

        DespesaOrcamentoMapper.updateEntity(despesa, "Frete adicional",
                new BigDecimal("50.00"));
        assertThat(despesa.getDescricao()).isEqualTo("Frete adicional");
        assertThat(despesa.getValor()).isEqualByComparingTo("50.00");
        assertThat(despesa.getOrcamentoVersao()).isSameAs(versao);
    }

    @Test
    void deveConverterTodosOsCamposParaResponse() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 23, 10, 0);
        DespesaOrcamento despesa = DespesaOrcamento.builder()
                .id(30L).descricao("Frete").valor(new BigDecimal("180.00"))
                .criadoEm(criadoEm).build();

        var response = DespesaOrcamentoMapper.toResponse(despesa);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("180.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }
}
