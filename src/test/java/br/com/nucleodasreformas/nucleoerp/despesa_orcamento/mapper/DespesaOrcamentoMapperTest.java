package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DespesaOrcamentoMapperTest {

    @Test
    void deveCriarEntidadeComOrcamentoDescricaoEValorResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();

        DespesaOrcamento resultado = DespesaOrcamentoMapper.toEntity(
                orcamento, "Frete", new BigDecimal("180.00"));

        assertThat(resultado.getOrcamento()).isSameAs(orcamento);
        assertThat(resultado.getDescricao()).isEqualTo("Frete");
        assertThat(resultado.getValor()).isEqualByComparingTo("180.00");
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getCriadoEm()).isNull();
    }

    @Test
    void deveAtualizarSomenteCamposEditaveis() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 21, 12, 0);
        DespesaOrcamento registro = DespesaOrcamento.builder()
                .id(20L)
                .orcamento(orcamento)
                .descricao("Frete")
                .valor(new BigDecimal("180.00"))
                .criadoEm(criadoEm)
                .build();

        DespesaOrcamentoMapper.updateEntity(
                registro, "Pedágio", new BigDecimal("50.00"));

        assertThat(registro.getId()).isEqualTo(20L);
        assertThat(registro.getOrcamento()).isSameAs(orcamento);
        assertThat(registro.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(registro.getDescricao()).isEqualTo("Pedágio");
        assertThat(registro.getValor()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveConverterEntidadeParaResponse() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 21, 12, 0);
        DespesaOrcamento registro = DespesaOrcamento.builder()
                .id(20L)
                .orcamento(Orcamento.builder().id(10L).build())
                .descricao("Frete")
                .valor(new BigDecimal("180.00"))
                .criadoEm(criadoEm)
                .build();

        DespesaOrcamentoResponse response = DespesaOrcamentoMapper.toResponse(registro);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("180.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }
}
